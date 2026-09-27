import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { initializeAppCheck, ReCaptchaEnterpriseProvider, type AppCheck } from "firebase/app-check";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || "AIzaSyBfI46YJApVNyiV4saVGO-OVsH9UgdDXfs",
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "openkrishi-ai.firebaseapp.com",
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || "openkrishi-ai",
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "openkrishi-ai.firebasestorage.app",
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "837358973413",
  appId: import.meta.env.VITE_FIREBASE_APP_ID || "1:837358973413:web:49f86614fd155f29ce3f0c",
};

export const app = initializeApp(firebaseConfig);

const recaptchaEnterpriseSiteKey = import.meta.env.VITE_RECAPTCHA_ENTERPRISE_SITE_KEY || "";

export const appCheck: AppCheck | null = recaptchaEnterpriseSiteKey
  ? initializeAppCheck(app, {
      provider: new ReCaptchaEnterpriseProvider(recaptchaEnterpriseSiteKey),
      isTokenAutoRefreshEnabled: true,
    })
  : null;

export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
