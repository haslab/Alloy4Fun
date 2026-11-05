import { test, expect } from '@playwright/test'

const PUBLIC_CODE = 'sig A {}\nrun { some A } for 3\n'
const BROKEN_CODE = 'sig A {\nrun { some A } for 3\n'
const SECRET_CODE = 'sig A {}\nrun { some A } for 3\n//SECRET\ncheck { all a: A | some a } for 3\n'

// Errors that the current app already produces; anything else fails the test.
const KNOWN_PAGE_ERRORS = [/Bootstrap's JavaScript requires jQuery/]

/** Opens the app and collects unexpected uncaught page errors. */
async function openApp(page, path = '/') {
    const errors = []
    page.on('pageerror', (e) => {
        if (!KNOWN_PAGE_ERRORS.some((re) => re.test(e.message))) errors.push(e.message)
    })
    await page.goto(path)
    await expect(page.locator('.CodeMirror')).toBeVisible()
    return errors
}

async function setCode(page, code) {
    await page.locator('.CodeMirror').evaluate((el, c) => el.CodeMirror.setValue(c), code)
}

async function getCode(page) {
    return page.locator('.CodeMirror').evaluate((el) => el.CodeMirror.getValue())
}

async function execute(page) {
    await page.locator('#exec > button').click()
}

async function shareModel(page) {
    await page.locator('#genUrl > button').click()
    await expect(page.locator('#url-permalink a.urlinfo').first()).toBeVisible()
    const links = await page.locator('#url-permalink a.urlinfo').allTextContents()
    return { public: links[0].trim(), private: links[1]?.trim() }
}

const pathOf = (url) => new URL(url, 'http://localhost').pathname

test.describe.configure({ mode: 'default' })

test('editor loads with its controls', async ({ page }) => {
    const errors = await openApp(page)
    await expect(page.locator('h1')).toContainText('Alloy4Fun')
    await expect(page.locator('#exec > button')).toBeVisible()
    await expect(page.locator('#genUrl > button')).toBeVisible()
    expect(errors).toEqual([])
})

test('executing a model shows an instance and navigation', async ({ page }) => {
    const errors = await openApp(page)
    await setCode(page, PUBLIC_CODE)
    await expect(page.locator('#exec > button')).toBeEnabled()
    await execute(page)
    await expect(page.locator('#instance canvas').first()).toBeVisible({ timeout: 60_000 })
    await expect(page.locator('#next > button')).toBeVisible()
    expect(errors).toEqual([])
})

test('a model with a syntax error is reported and not executed', async ({ page }) => {
    await openApp(page)
    await setCode(page, BROKEN_CODE)
    await execute(page)
    await expect(page.locator('#log p.log-error').first()).toBeVisible({ timeout: 30_000 })
    await expect(page.locator('#instance canvas')).toHaveCount(0)
})

test('sharing a model gives a link that reloads the same model', async ({ page, context }) => {
    const errors = await openApp(page)
    await setCode(page, PUBLIC_CODE)
    const links = await shareModel(page)
    expect(links.public).toMatch(/\/[A-Za-z0-9]{17}$/)

    const reopened = await context.newPage()
    await openApp(reopened, pathOf(links.public))
    await expect.poll(() => getCode(reopened)).toBe(PUBLIC_CODE.trimEnd())
    expect(errors).toEqual([])
})

test('secrets stay hidden on public links and visible on private links', async ({ page, context }) => {
    await openApp(page)
    await setCode(page, SECRET_CODE)
    const links = await shareModel(page)
    expect(links.private).toBeTruthy()

    const publicPage = await context.newPage()
    await openApp(publicPage, pathOf(links.public))
    await expect.poll(() => getCode(publicPage)).not.toContain('//SECRET')
    expect(await getCode(publicPage)).not.toContain('check { all a: A')

    const privatePage = await context.newPage()
    await openApp(privatePage, pathOf(links.private))
    await expect.poll(() => getCode(privatePage)).toContain('//SECRET')
})

test('a shared instance can be reopened from its link', async ({ page, context }) => {
    await openApp(page)
    await setCode(page, PUBLIC_CODE)
    await execute(page)
    await expect(page.locator('#instance canvas').first()).toBeVisible({ timeout: 60_000 })
    const link = page.locator('#url-instance-permalink a.urlinfo')
    // an early click can be lost while the instance is still being rendered
    let href
    await expect(async () => {
        if (!(await link.isVisible())) await page.locator('#genInstanceUrl > button').click({ timeout: 2_000 })
        await expect(link).toBeVisible({ timeout: 4_000 })
        href = await link.getAttribute('href', { timeout: 2_000 })
    }).toPass({ timeout: 30_000 })

    const reopened = await context.newPage()
    await openApp(reopened, pathOf(href))
    await expect(reopened.locator('#instance canvas').first()).toBeVisible({ timeout: 30_000 })
})

test('the derivation tree of a private link can be downloaded', async ({ page }) => {
    await openApp(page)
    await setCode(page, SECRET_CODE)
    const links = await shareModel(page)
    const privatePage = await page.context().newPage()
    await openApp(privatePage, pathOf(links.private))
    await expect(privatePage.locator('#downloadTree > button')).toBeEnabled()
    const download = privatePage.waitForEvent('download')
    await privatePage.locator('#downloadTree > button').click()
    expect((await download).suggestedFilename()).toBeTruthy()
})

test('the statistics page loads for a shared model', async ({ page, context }) => {
    const errors = await openApp(page)
    await setCode(page, PUBLIC_CODE)
    const links = await shareModel(page)

    const stats = await context.newPage()
    const statsErrors = []
    stats.on('pageerror', (e) => {
        if (!KNOWN_PAGE_ERRORS.some((re) => re.test(e.message))) statsErrors.push(e.message)
    })
    await stats.goto(`/stats${pathOf(links.public)}`)
    await expect(stats.locator('h1')).toContainText('Alloy4Fun')
    await expect(stats.locator('#graph')).toBeAttached({ timeout: 30_000 })
    expect(statsErrors).toEqual([])
    expect(errors).toEqual([])
})

test('an unknown link opens an empty editor without errors', async ({ page }) => {
    const errors = await openApp(page, '/doesNotExist00000')
    await expect(page.locator('#exec > button')).toBeVisible()
    expect((await getCode(page)).trim()).toBe('')
    expect(errors).toEqual([])
})

test('a path matching no route shows the not-found page', async ({ page }) => {
    await page.goto('/no/such/page/here')
    await expect(page.locator('body')).toContainText('404')
})

test('clicking a shared link navigates in-app without reloading', async ({ page }) => {
    await openApp(page)
    await setCode(page, PUBLIC_CODE)
    const links = await shareModel(page)
    await page.evaluate(() => { window.__marker = 'same-page' })
    page.on('dialog', (d) => d.dismiss())
    await page.locator('#url-permalink a.urlinfo').first().click()
    await expect.poll(() => new URL(page.url()).pathname).toBe(pathOf(links.public))
    await expect.poll(() => getCode(page)).toContain('sig A')
    expect(await page.evaluate(() => window.__marker)).toBe('same-page')
})
