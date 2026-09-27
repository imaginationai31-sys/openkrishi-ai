import { useEffect, useRef, useState } from "react";
import { onAuthStateChanged, signInAnonymously, type User } from "firebase/auth";
import { auth } from "./firebase";
import { upsertUserProfile } from "./services/profile";
import { assessCropImage, type VisionAssessment } from "./services/diagnosis";
import { playBase64Audio, sendVoiceAdvisory, type VoiceAdvisoryResult } from "./services/voice";

const languages = [["bn", "বাংলা"], ["hi", "हिन्दी"], ["ta", "தமிழ்"], ["pa", "ਪੰਜਾਬੀ"], ["te", "తెలుగు"]] as const;
const crops = [["rice", "Rice", "🌾"], ["peanut", "Peanut", "🥜"], ["vegetables", "Vegetables", "🥬"], ["flowers", "Flowers", "🌼"]] as const;

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [language, setLanguage] = useState("hi");
  const [busy, setBusy] = useState(true);
  const [authError, setAuthError] = useState("");
  const [screen, setScreen] = useState<"home" | "diagnose">("home");
  const [crop, setCrop] = useState("rice");
  const [growthStage, setGrowthStage] = useState("");
  const [photo, setPhoto] = useState<File | null>(null);
  const [preview, setPreview] = useState("");
  const [assessment, setAssessment] = useState<VisionAssessment | null>(null);
  const [diagnosing, setDiagnosing] = useState(false);
  const [recording, setRecording] = useState(false);
  const [voiceBusy, setVoiceBusy] = useState(false);
  const [voiceResult, setVoiceResult] = useState<VoiceAdvisoryResult | null>(null);
  const [voiceError, setVoiceError] = useState("");
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);
  const [diagnosisError, setDiagnosisError] = useState("");
  const cameraRef = useRef<HTMLInputElement>(null);
  const galleryRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let active = true;
    const unsubscribe = onAuthStateChanged(auth, async (current) => {
      if (!active) return;
      try {
        let authenticatedUser = current;
        if (!authenticatedUser) authenticatedUser = (await signInAnonymously(auth)).user;
        if (!active) return;
        setUser(authenticatedUser);
        try {
          await upsertUserProfile(authenticatedUser, language);
          if (active) setAuthError("");
        } catch (error) {
          console.error("Failed to save Firebase user profile:", error);
          if (active) setAuthError("Your session is active, but profile sync is temporarily unavailable.");
        }
      } catch (error) {
        console.error("Firebase anonymous sign-in failed:", error);
        if (active) {
          setUser(null);
          setAuthError("Unable to connect to Firebase. Please check your connection and try again.");
        }
      } finally {
        if (active) setBusy(false);
      }
    });
    return () => { active = false; unsubscribe(); };
  }, []);

  useEffect(() => {
    if (!user) return;
    upsertUserProfile(user, language).catch((error) => console.error("Failed to update language:", error));
  }, [user, language]);

  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview); }, [preview]);

  const openVoice = () => { setScreen("voice"); setVoiceError(""); setVoiceResult(null); };

  const startRecording = async () => {
    setVoiceError(""); setVoiceResult(null);
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") { setVoiceError("Voice recording is not supported in this browser."); return; }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mimeTypes = ["audio/webm;codecs=opus", "audio/webm", "audio/mp4"];
      const mimeType = mimeTypes.find((type) => MediaRecorder.isTypeSupported(type)) || "";
      const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);
      chunksRef.current = []; streamRef.current = stream; mediaRecorderRef.current = recorder;
      recorder.ondataavailable = (event) => { if (event.data.size > 0) chunksRef.current.push(event.data); };
      recorder.onstop = async () => {
        stream.getTracks().forEach((track) => track.stop()); streamRef.current = null;
        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" });
        if (!blob.size) { setVoiceError("No voice audio was captured. Please try again."); return; }
        setVoiceBusy(true);
        try {
          const result = await sendVoiceAdvisory(blob, language, crop, growthStage);
          setVoiceResult(result);
          if (user) await upsertUserProfile(user, language, crop);
          if (result.audio?.base64) playBase64Audio(result.audio.base64, result.audio.mime_type || "audio/wav");
        } catch (error) { setVoiceError(error instanceof Error ? error.message : "Voice advisory failed. Please try again."); }
        finally { setVoiceBusy(false); }
      };
      recorder.start(); setRecording(true);
    } catch (error) { console.error("Microphone permission failed:", error); setVoiceError("Microphone access was not granted. Allow microphone permission and try again."); }
  };

  const stopRecording = () => { if (mediaRecorderRef.current && recording) { mediaRecorderRef.current.stop(); mediaRecorderRef.current = null; setRecording(false); } };

  const openDiagnosis = (cropCategory = crop) => {
    setCrop(cropCategory);
    setScreen("diagnose");
    setAssessment(null);
    setDiagnosisError("");
  };

  const selectPhoto = (file?: File) => {
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setDiagnosisError("Please choose an image.");
      return;
    }
    if (file.size > 12 * 1024 * 1024) {
      setDiagnosisError("Image must be smaller than 12 MB.");
      return;
    }
    if (preview) URL.revokeObjectURL(preview);
    setPhoto(file);
    setPreview(URL.createObjectURL(file));
    setAssessment(null);
    setDiagnosisError("");
  };

  const runDiagnosis = async () => {
    if (!photo) {
      setDiagnosisError("Take or choose a crop photo first.");
      return;
    }
    setDiagnosing(true);
    setDiagnosisError("");
    try {
      const result = await assessCropImage(photo, { cropCategory: crop, language, growthStage });
      setAssessment(result);
      if (user) {
        await upsertUserProfile(user, language, crop);
      }
    } catch (error) {
      setDiagnosisError(error instanceof Error ? error.message : "Crop assessment failed. Please try again.");
    } finally {
      setDiagnosing(false);
    }
  };

  if (busy) {
    return <main className="splash"><div className="logo">🌱</div><h1>OpenKrishi AI</h1><p>Preparing your farm assistant…</p></main>;
  }

  if (screen === "voice") {
    return <main className="shell">
      <header className="topbar"><button className="back-button" onClick={() => setScreen("home")}>← Home</button><span className="status">{user ? "Connected" : "Offline"}</span></header>
      <section className="voice-hero"><p className="eyebrow">VOICE AGRONOMY</p><h1>Ask your farm assistant</h1><p>Speak naturally in your selected language. OpenKrishi will understand, advise, and speak the answer back.</p></section>
      <section className="panel"><div className="section-heading"><h2>Voice language</h2><span>{languages.find(([code]) => code === language)?.[1]}</span></div><div className="language-grid">{languages.map(([code,label]) => <button className={language===code ? "language active" : "language"} onClick={()=>setLanguage(code)} key={code}>{label}</button>)}</div></section>
      <section className="panel"><div className="section-heading"><h2>Crop context</h2><span>Optional</span></div><div className="crop-picker">{crops.map(([id,label,icon])=><button className={crop===id ? "crop-option active":"crop-option"} key={id} onClick={()=>setCrop(id)}><span>{icon}</span>{label}</button>)}</div><label className="field-label" htmlFor="voice-growth">Growth stage <span>optional</span></label><input id="voice-growth" className="text-input" value={growthStage} onChange={e=>setGrowthStage(e.target.value)} placeholder="e.g. seedling, tillering, flowering"/></section>
      <section className="panel voice-panel"><button className={recording ? "record-button recording":"record-button"} onClick={recording ? stopRecording : startRecording} aria-label={recording ? "Stop recording":"Start recording"}>🎙️</button><h2>{recording ? "Listening…" : voiceBusy ? "Preparing your advisory…" : "Tap to speak"}</h2><p>{recording ? "Speak clearly about your crop, then tap again when finished.":"Your browser will request microphone permission the first time."}</p></section>
      {voiceError && <div className="notice error" role="alert">{voiceError}</div>}
      {voiceResult && <section className="panel result-panel"><div className="result-status"><span>Transcription</span><strong>{voiceResult.transcription.confidence} confidence</strong></div><p className="transcription">“{voiceResult.transcription.text}”</p><h2>{voiceResult.advisory.answer}</h2>{voiceResult.advisory.observations.length>0&&<ResultList title="Observations" items={voiceResult.advisory.observations}/>}<ResultList title="Safe next steps" items={voiceResult.advisory.recommendations}/>{voiceResult.advisory.uncertainties.length>0&&<ResultList title="Important limitations" items={voiceResult.advisory.uncertainties}/>} {voiceResult.audio?.base64&&<button className="secondary-action replay-button" onClick={()=>playBase64Audio(voiceResult.audio!.base64,voiceResult.audio!.mime_type||"audio/wav")}>🔊 Play again</button>}</section>}
      <nav className="bottom-nav"><button onClick={()=>setScreen("home")}>⌂<span>Home</span></button><button onClick={()=>openDiagnosis()}>📷<span>Diagnose</span></button><button className="nav-active">🎙️<span>Voice</span></button><button>📚<span>History</span></button></nav>
    </main>;
  }

  if (screen === "diagnose") {
    return (
      <main className="shell">
        <header className="topbar">
          <button className="back-button" onClick={() => setScreen("home")}>← Home</button>
          <span className="status">{user ? "Connected" : "Offline"}</span>
        </header>

        <section className="diagnose-hero">
          <p className="eyebrow">AI CROP DIAGNOSIS</p>
          <h1>Show us your crop</h1>
          <p>Take a clear photo of affected leaves, stems, flowers, or fruit.</p>
        </section>

        <section className="panel">
          <div className="section-heading"><h2>Crop</h2><span>Required</span></div>
          <div className="crop-picker">
            {crops.map(([id, label, icon]) => (
              <button className={crop === id ? "crop-option active" : "crop-option"} key={id} onClick={() => setCrop(id)}>
                <span>{icon}</span>{label}
              </button>
            ))}
          </div>
          <label className="field-label" htmlFor="growth-stage">Growth stage <span>optional</span></label>
          <input id="growth-stage" className="text-input" value={growthStage} onChange={(e) => setGrowthStage(e.target.value)} placeholder="e.g. seedling, tillering, flowering" />
        </section>

        <section className="panel">
          {preview ? (
            <div className="photo-preview">
              <img src={preview} alt="Selected crop" />
              <button className="remove-photo" onClick={() => { if (preview) URL.revokeObjectURL(preview); setPreview(""); setPhoto(null); setAssessment(null); }}>Remove photo</button>
            </div>
          ) : (
            <div className="photo-empty">
              <div className="camera-icon">📷</div>
              <h2>Add crop photo</h2>
              <p>Use your camera or select an image from your gallery.</p>
              <div className="photo-actions">
                <button className="primary-action" onClick={() => cameraRef.current?.click()}>📷 Camera</button>
                <button className="secondary-action" onClick={() => galleryRef.current?.click()}>🖼️ Gallery</button>
              </div>
            </div>
          )}
          <input ref={cameraRef} className="hidden-input" type="file" accept="image/*" capture="environment" onChange={(e) => selectPhoto(e.target.files?.[0])} />
          <input ref={galleryRef} className="hidden-input" type="file" accept="image/*" onChange={(e) => selectPhoto(e.target.files?.[0])} />
        </section>

        {diagnosisError && <div className="notice error" role="alert">{diagnosisError}</div>}

        <button className="diagnose-button" disabled={!photo || diagnosing} onClick={runDiagnosis}>
          {diagnosing ? "Analyzing crop…" : "🔎 Analyze crop"}
        </button>

        {assessment && (
          <section className="panel result-panel">
            <div className="result-status"><span>AI assessment</span><strong>{assessment.vision.confidence} confidence</strong></div>
            <h2>{assessment.advisory.answer}</h2>
            {assessment.vision.observations.length > 0 && <ResultList title="What we can see" items={assessment.vision.observations} />}
            {assessment.vision.possible_causes.length > 0 && <ResultList title="Possible causes" items={assessment.vision.possible_causes} />}
            <ResultList title="Recommended checks" items={assessment.advisory.recommendations} />
            {assessment.advisory.uncertainties.length > 0 && <ResultList title="Important limitations" items={assessment.advisory.uncertainties} />}
          </section>
        )}

        <nav className="bottom-nav">
          <button onClick={() => setScreen("home")}>⌂<span>Home</span></button>
          <button className="nav-active">📷<span>Diagnose</span></button>
          <button onClick={openVoice}>🎙️<span>Voice</span></button>
          <button>📚<span>History</span></button>
        </nav>
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div><div className="brand">🌱 OpenKrishi AI</div><div className="subtitle">Vernacular AI Agronomy & Voice Advisory</div></div>
        <span className="status">{user ? "Connected" : "Offline"}</span>
      </header>
      {authError && <div className="notice" role="status">{authError}</div>}
      <section className="hero">
        <div><p className="eyebrow">YOUR FARM ASSISTANT</p><h1>Understand your crop. Act with confidence.</h1><p>Take a crop photo, ask by voice, and receive localized agricultural guidance.</p></div>
        <div className="hero-icon">🌾</div>
      </section>
      <section className="panel">
        <div className="section-heading"><h2>Choose language</h2><span>5 languages</span></div>
        <div className="language-grid">{languages.map(([code, label]) => <button className={language === code ? "language active" : "language"} onClick={() => setLanguage(code)} key={code}>{label}</button>)}</div>
      </section>
      <section className="panel">
        <div className="section-heading"><h2>Crop intelligence</h2><span>Photo diagnosis</span></div>
        <div className="crop-grid">{crops.map(([id, label, icon]) => <button className="crop-card" key={id} onClick={() => openDiagnosis(id)}><span className="crop-icon">{icon}</span><strong>{label}</strong><small>AI diagnosis & advice</small></button>)}</div>
      </section>
      <button className="main-diagnose" onClick={() => openDiagnosis()}>📷 Start crop diagnosis</button>
      <nav className="bottom-nav">
        <button className="nav-active">⌂<span>Home</span></button>
        <button onClick={() => openDiagnosis()}>📷<span>Diagnose</span></button>
        <button>🎙️<span>Voice</span></button>
        <button>📚<span>History</span></button>
      </nav>
    </main>
  );
}

function ResultList({ title, items }: { title: string; items: string[] }) {
  return <div className="result-list"><h3>{title}</h3><ul>{items.map((item, index) => <li key={index}>{item}</li>)}</ul></div>;
}
