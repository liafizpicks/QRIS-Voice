export default async function handler(req, res) {
  try {
    const token = process.env.TELEGRAM_BOT_TOKEN;

    if (!token) {
      return res.status(500).json({
        ok: false,
        error: "TELEGRAM_BOT_TOKEN belum tersedia"
      });
    }

    const response = await fetch(
      `https://api.telegram.org/bot${token}/getWebhookInfo`
    );

    const data = await response.json();

    return res.status(200).json(data);

  } catch (error) {
    return res.status(500).json({
      ok: false,
      error: "Server error"
    });
  }
}
