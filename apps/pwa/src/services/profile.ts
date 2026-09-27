import { doc, serverTimestamp, setDoc } from "firebase/firestore";
import type { User } from "firebase/auth";
import { db } from "../firebase";

export async function upsertUserProfile(
  user: User,
  language: string,
  cropCategory?: string,
) {
  const profileRef = doc(db, "users", user.uid);
  await setDoc(
    profileRef,
    {
      uid: user.uid,
      authProvider: user.isAnonymous ? "anonymous" : "firebase",
      language,
      ...(cropCategory ? { lastCropCategory: cropCategory } : {}),
      updatedAt: serverTimestamp(),
    },
    { merge: true },
  );
}
