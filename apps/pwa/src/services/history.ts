import { addDoc, collection, getDocs, limit, orderBy, query, serverTimestamp } from "firebase/firestore";
import { db } from "../firebase";

export type HistoryRecord = {
  id: string;
  type: "vision" | "voice";
  language: string;
  cropCategory?: string;
  growthStage?: string;
  question?: string;
  possibleCauses?: string[];
  confidence?: string;
  advisory: { answer: string; observations: string[]; recommendations: string[]; uncertainties: string[]; confidence?: string; safety?: { status?: string; reasons?: string[] } };
  createdAt?: unknown;
};

type SaveInput = Omit<HistoryRecord, "id" | "createdAt" | "confidence">;

export async function saveDiagnosisHistory(uid: string, input: SaveInput) {
  const ref = collection(db, "users", uid, "diagnoses");
  await addDoc(ref, { ...input, confidence: input.advisory.confidence || "unknown", createdAt: serverTimestamp() });
}

export async function listHistory(uid: string): Promise<HistoryRecord[]> {
  const ref = collection(db, "users", uid, "diagnoses");
  const snapshot = await getDocs(query(ref, orderBy("createdAt", "desc"), limit(50)));
  return snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as Omit<HistoryRecord, "id">) }));
}
