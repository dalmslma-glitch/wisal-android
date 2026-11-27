const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

exports.lookupUserByPhone = onCall(async (request) => {
  if (!request.auth) throw new HttpsError("unauthenticated", "يجب تسجيل الدخول أولاً.");
  const phone = String(request.data.phone || "").replace(/[\s()-]/g, "");
  if (!/^\+[1-9]\d{7,14}$/.test(phone)) {
    throw new HttpsError("invalid-argument", "أدخل الرقم بصيغة دولية صحيحة.");
  }
  const result = await getFirestore().collection("users").where("phone", "==", phone).limit(1).get();
  const user = result.docs.find((doc) => doc.id !== request.auth.uid);
  if (!user) return { found: false };
  return {
    found: true,
    uid: user.id,
    displayName: user.get("displayName") || "جهة اتصال وصال",
  };
});

exports.notifyChatMember = onDocumentCreated(
  "chats/{chatId}/messages/{messageId}",
  async (event) => {
    const message = event.data.data();
    const chatId = event.params.chatId;
    if (!message || !message.senderId) return;
    const db = getFirestore();
    const chatSnap = await db.collection("chats").doc(chatId).get();
    const members = chatSnap.get("members") || [];
    const recipients = members.filter((uid) => uid !== message.senderId);
    if (!recipients.length) return;
    const users = await Promise.all(recipients.map((uid) => db.collection("users").doc(uid).get()));
    const tokens = users.map((user) => user.get("fcmToken")).filter(Boolean);
    if (!tokens.length) return;
    const sender = await db.collection("users").doc(message.senderId).get();
    const senderName = sender.get("displayName") || "رسالة جديدة";
    await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title: senderName, body: message.type === "text" ? (message.text || "رسالة جديدة") : "أرسل وسائط إليك" },
      data: { chatId, senderId: message.senderId },
      android: { priority: "high", notification: { channelId: "wisal_messages", sound: "default" } },
    });
  }
);
