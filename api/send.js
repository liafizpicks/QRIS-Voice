export default async function handler(req, res) {
  if (req.method !== "POST") {
    return res.status(405).json({ ok: false, error: "Method not allowed" });
  }

  try {
    const { message } = req.body || {};

    if (!message) {
      return res.status(400).json({ ok: false, error: "message wajib diisi" });
    }

    const token = process.env.TELEGRAM_BOT_TOKEN;

    if (!token) {
      return res.status(500).json({ ok: false, error: "Telegram token belum tersedia" });
    }

    const chatId = process.env.TELEGRAM_CHAT_ID;

    if (!chatId) {
      return res.status(500).json({ ok: false, error: "Telegram chat ID belum tersedia" });
    }

    const response = await fetch(
      `https://api.telegram.org/bot${token}/sendMessage`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          chat_id: chatId,
          text: message
        })
      }
    );

    const data = await response.json();

    if (!response.ok || !data.ok) {
      return res.status(500).json({
        ok: false,
        error: "Telegram gagal mengirim pesan"
      });
    }

    return res.status(200).json({ ok: true });
  } catch (error) {
    return res.status(500).json({
      ok: false,
      error: "Server error"
    });
  }
}
