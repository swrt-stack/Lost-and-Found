import { mkdir } from 'node:fs/promises'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import puppeteer from 'puppeteer'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..')
const OUT_DIR = join(ROOT, 'thesis_figures', 'ui')
const BASE = process.env.THESIS_UI_BASE || 'http://localhost:5173'
const API = process.env.THESIS_API_BASE || 'http://localhost:8080'

async function login(username, password) {
  const capRes = await fetch(`${API}/api/auth/captcha`)
  const cap = (await capRes.json()).data
  const svg = Buffer.from(cap.imageData.split(',')[1], 'base64').toString('utf8')
  const code = [...svg.matchAll(/>([A-Z0-9])<\/text>/g)].map((m) => m[1]).join('')
  const loginRes = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username,
      password,
      captchaId: cap.captchaId,
      captchaCode: code,
    }),
  })
  return (await loginRes.json()).data
}

function authBridgeUrl(session, nextPath) {
  const params = new URLSearchParams({
    token: session.token,
    role: session.role,
    username: session.username,
    next: nextPath,
  })
  return `${BASE}/thesis-auth.html?${params.toString()}`
}

const shots = [
  ['fig4_14_home.png', '/', false],
  ['fig4_15_items.png', '/items', true],
  ['fig4_16_search.png', `/search?keyword=${encodeURIComponent('耳机')}`, true],
  ['fig4_17_detail.png', '/item-detail?id=LOST-1', true],
  ['fig4_18_admin_announcements.png', '/admin-announcements', 'admin'],
]

await mkdir(OUT_DIR, { recursive: true })

const browser = await puppeteer.launch({
  headless: true,
  defaultViewport: { width: 1440, height: 960 },
  args: ['--no-sandbox', '--disable-dev-shm-usage'],
})

try {
  const page = await browser.newPage()
  page.on('dialog', async (dialog) => {
    console.warn('dialog:', dialog.message())
    await dialog.dismiss()
  })

  const demoSession = await login('demo', '123456')

  for (const [name, path, authMode] of shots) {
    console.log('capture', name, path)
    let target = `${BASE}${path}`
    if (authMode === 'admin') {
      target = authBridgeUrl(await login('sysadmin', '123456'), path)
    } else if (authMode === true) {
      target = authBridgeUrl(demoSession, path)
    }
    await page.goto(target, { waitUntil: 'networkidle2', timeout: 60000 })
    if (authMode === 'admin') {
      await page.waitForSelector('.admin-main-card, .admin-table', { timeout: 30000 })
    } else if (name.includes('detail')) {
      await page.waitForSelector('.detail-grid', { timeout: 30000 })
    } else if (name.includes('search')) {
      await page.waitForSelector('.search-page-grid, .page-heading', { timeout: 30000 })
      await new Promise((r) => setTimeout(r, 1500))
    } else if (name.includes('items')) {
      await page.waitForSelector('.page-heading', { timeout: 30000 })
      await page.waitForFunction(
        () => document.querySelectorAll('.results-table tbody tr').length > 0,
        { timeout: 30000 },
      )
    } else {
      await page.waitForSelector('.hero-home, .home-item-card', { timeout: 30000 })
    }
    await page.screenshot({ path: join(OUT_DIR, name), fullPage: false })
    console.log('  saved', name)
  }
} finally {
  await browser.close()
}
