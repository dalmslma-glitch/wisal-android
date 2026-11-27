import { AwsClient } from "aws4fetch";

const PROJECT_ID = "wisal-f7bb5";
const REGION = "us-east-005";
const S3_ENDPOINT = "https://s3.us-east-005.backblazeb2.com";
const MAX_FILE_BYTES = 25 * 1024 * 1024;
const ALLOWED_PREFIXES = ["image/", "video/", "audio/"];
const ALLOWED_EXACT = new Set([
  "application/pdf", "text/plain", "application/zip",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.ms-excel",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "application/vnd.ms-powerpoint",
  "application/vnd.openxmlformats-officedocument.presentationml.presentation"
]);

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
  });
}
function b64url(value) {
  return Uint8Array.from(atob(value.replace(/-/g, "+").replace(/_/g, "/")), c => c.charCodeAt(0));
}
function decodePart(value) {
  return JSON.parse(new TextDecoder().decode(b64url(value)));
}
async function verifyFirebaseToken(request) {
  const auth = request.headers.get("Authorization") || "";
  if (!auth.startsWith("Bearer ")) throw new Error("تسجيل الدخول مطلوب.");
  const token = auth.slice(7);
  const parts = token.split(".");
  if (parts.length !== 3) throw new Error("رمز الدخول غير صالح.");
  const header = decodePart(parts[0]);
  const claims = decodePart(parts[1]);
  const now = Math.floor(Date.now() / 1000);
  if (header.alg !== "RS256" || !header.kid) throw new Error("خوارزمية رمز الدخول غير مدعومة.");
  if (claims.aud !== PROJECT_ID || claims.iss !== `https://securetoken.google.com/${PROJECT_ID}` ||
      !claims.sub || claims.sub.length > 128 || claims.exp <= now || claims.iat > now + 60) {
    throw new Error("انتهت صلاحية رمز الدخول أو لا يخص مشروع وصال.");
  }
  const keyResponse = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com");
  if (!keyResponse.ok) throw new Error("تعذر التحقق من حساب Firebase.");
  const keys = await keyResponse.json();
  const jwk = keys.keys.find(k => k.kid === header.kid);
  if (!jwk) throw new Error("مفتاح التحقق غير معروف.");
  const publicKey = await crypto.subtle.importKey("jwk", jwk, {
    name: "RSASSA-PKCS1-v1_5", hash: "SHA-256"
  }, false, ["verify"]);
  const valid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5", publicKey, b64url(parts[2]),
    new TextEncoder().encode(parts[0] + "." + parts[1])
  );
  if (!valid) throw new Error("تعذر التحقق من هوية المستخدم.");
  return claims.sub;
}
function storageClient(env) {
  if (!env.B2_KEY_ID || !env.B2_APPLICATION_KEY || !env.B2_BUCKET_NAME) {
    throw new Error("إعدادات التخزين غير مكتملة على الخادم.");
  }
  return new AwsClient({
    accessKeyId: env.B2_KEY_ID,
    secretAccessKey: env.B2_APPLICATION_KEY,
    service: "s3",
    region: REGION
  });
}
function objectUrl(bucket, key) {
  const safeKey = key.split("/").map(encodeURIComponent).join("/");
  return new URL(`/${encodeURIComponent(bucket)}/${safeKey}`, S3_ENDPOINT);
}
function allowedMime(mime) {
  return ALLOWED_PREFIXES.some(prefix => mime.startsWith(prefix)) || ALLOWED_EXACT.has(mime);
}
function extension(name, mime) {
  const ext = (name.split(".").pop() || "").toLowerCase().replace(/[^a-z0-9]/g, "").slice(0, 10);
  if (ext) return ext;
  return mime.startsWith("image/") ? "img" : mime.startsWith("video/") ? "vid" : "bin";
}
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: {
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
        "Access-Control-Allow-Headers": "Authorization, Content-Type"
      }});
    }
    try {
      if (request.method === "GET" && url.pathname === "/health") return json({ status: "ok", service: "wisal-media-worker" });
      if (request.method === "POST" && url.pathname === "/v1/upload-ticket") {
        const uid = await verifyFirebaseToken(request);
        const body = await request.json();
        const mime = String(body.mimeType || "application/octet-stream").toLowerCase();
        const name = String(body.fileName || "attachment").slice(0, 120);
        const size = Number(body.size || 0);
        if (!allowedMime(mime)) return json({ error: "نوع الملف غير مسموح." }, 415);
        if (!Number.isFinite(size) || size <= 0 || size > MAX_FILE_BYTES) {
          return json({ error: "حجم الملف يجب ألا يتجاوز 25 ميغابايت." }, 413);
        }
        const key = `chat-media/${uid}/${crypto.randomUUID()}.${extension(name, mime)}`;
        const target = objectUrl(env.B2_BUCKET_NAME, key);
        const signed = await storageClient(env).sign(target, {
          method: "PUT",
          headers: { "Content-Type": mime }
        }, { aws: { signQuery: true }, expiresIn: 300 });
        return json({
          uploadUrl: signed.url,
          mediaUrl: `${url.origin}/v1/media/${key}`,
          objectKey: key
        });
      }
      if (request.method === "GET" && url.pathname.startsWith("/v1/media/")) {
        const key = url.pathname.slice("/v1/media/".length);
        if (!key.startsWith("chat-media/") || key.includes("..")) return json({ error: "الملف غير موجود." }, 404);
        const signed = await storageClient(env).sign(objectUrl(env.B2_BUCKET_NAME, key), {
          method: "GET"
        }, { aws: { signQuery: true }, expiresIn: 120 });
        return Response.redirect(signed.url, 302);
      }
      return json({ error: "المسار غير موجود." }, 404);
    } catch (error) {
      return json({ error: error?.message || "حدث خطأ غير متوقع." }, 400);
    }
  }
};
