import { useEffect, useState } from "react";
import { onAuthStateChanged, signInAnonymously, type User } from "firebase/auth";
import { auth } from "./firebase";
import { upsertUserProfile } from "./services/profile";

const languages = [["bn", "বাংলা"], ["hi", "हिन्दी"], ["ta", "தமிழ்"], ["pa", "ਪੰਜਾਬੀ"], ["te", "తెలుగు"]] as const;
const crops = [["rice", "Rice", "🌾"], ["peanut", "Peanut", "🥜"], ["vegetables", "Vegetables", "🥬"], ["flowers", "Flowers", "🌼"]] as const;

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [language, setLanguage] = useState("hi");
  const [busy, setBusy] = useState(true);
  const [authError, setAuthError] = useState("");

  useEffect(() => {
    let active = true;

    const unsubscribe = onAuthStateChanged(auth, async (current) => {
      if (!active) return;

      try {
        let authenticatedUser = current;

        if (!authenticatedUser) {
          const result = await signInAnonymously(auth);
          authenticatedUser = result.user;
        }

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

    return () => {
      active = false;
      unsubscribe();
    };
  }, []);

  useEffect(() => {
    if (!user) return;
    upsertUserProfile(user, language).catch((error) => {
      console.error("Failed to update language preference:", error);
    });
  }, [user, language]);

  const selectCrop = (cropCategory: string) => {
    if (!user) return;
    upsertUserProfile(user, language, cropCategory).catch((error) => {
      console.error("Failed to save crop preference:", error);
    });
  };

  if (busy) {
    return (
      <main className="splash">
        <div className="logo">🌱</div>
        <h1>OpenKrishi AI</h1>
        <p>Preparing your farm assistant…</p>
      </main>
    );
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div>
          <div className="brand">🌱 OpenKrishi AI</div>
          <div className="subtitle">Vernacular AI Agronomy & Voice Advisory</div>
        </div>
        <span className="status">{user ? "Connected" : "Offline"}</span>
      </header>

      {authError && <div className="notice" role="status">{authError}</div>}

      <section className="hero">
        <div>
          <p className="eyebrow">YOUR FARM ASSISTANT</p>
          <h1>Understand your crop. Act with confidence.</h1>
          <p>Take a crop photo, ask by voice, and receive localized agricultural guidance.</p>
        </div>
        <div className="hero-icon">🌾</div>
      </section>

      <section className="panel">
        <div className="section-heading"><h2>Choose language</h2><span>5 languages</span></div>
        <div className="language-grid">
          {languages.map(([code, label]) => (
            <button className={language === code ? "language active" : "language"} onClick={() => setLanguage(code)} key={code}>
              {label}
            </button>
          ))}
        </div>
      </section>

      <section className="panel">
        <div className="section-heading"><h2>Crop intelligence</h2><span>Photo or voice</span></div>
        <div className="crop-grid">
          {crops.map(([id, label, icon]) => (
            <button className="crop-card" key={id} onClick={() => selectCrop(id)}>
              <span className="crop-icon">{icon}</span>
              <strong>{label}</strong>
              <small>AI diagnosis & advice</small>
            </button>
          ))}
        </div>
      </section>

      <nav className="bottom-nav">
        <button className="nav-active">⌂<span>Home</span></button>
        <button>📷<span>Diagnose</span></button>
        <button>🎙️<span>Voice</span></button>
        <button>📚<span>History</span></button>
      </nav>
    </main>
  );
}
