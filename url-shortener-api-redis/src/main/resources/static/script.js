// DOM Elements
const urlInput = document.getElementById("urlInput");
const shortenBtn = document.getElementById("shortenBtn");
const resultBox = document.getElementById("resultBox");
const shortUrlDisplay = document.getElementById("shortUrlDisplay");
const copyBtn = document.getElementById("copyBtn");
const copyText = document.getElementById("copyText");
const visitBtn = document.getElementById("visitBtn");
const quickBenchmarkBtn = document.getElementById("quickBenchmarkBtn");

// Benchmark DOM Elements
const activeShortCode = document.getElementById("activeShortCode");
const activeLongUrl = document.getElementById("activeLongUrl");
const runBenchmarkBtn = document.getElementById("runBenchmarkBtn");
const runBenchmarkIcon = document.getElementById("runBenchmarkIcon");
const runBenchmarkText = document.getElementById("runBenchmarkText");
const evictCacheBtn = document.getElementById("evictCacheBtn");
const testDbOnlyBtn = document.getElementById("testDbOnlyBtn");
const testRedisOnlyBtn = document.getElementById("testRedisOnlyBtn");

// Metrics DOM Elements
const noRedisLatency = document.getElementById("noRedisLatency");
const noRedisBar = document.getElementById("noRedisBar");
const redisLatency = document.getElementById("redisLatency");
const redisBar = document.getElementById("redisBar");
const redisStatusTag = document.getElementById("redisStatusTag");

// Insights DOM Elements
const speedupValue = document.getElementById("speedupValue");
const reductionValue = document.getElementById("reductionValue");
const savedMsValue = document.getElementById("savedMsValue");
const insightSummary = document.getElementById("insightSummary");

// History Table
const logsTableBody = document.getElementById("logsTableBody");
const clearLogsBtn = document.getElementById("clearLogsBtn");
const toast = document.getElementById("toast");

let currentShortCode = null;
let currentLongUrl = null;
let isBenchmarking = false;

// 1. Create Short URL
shortenBtn.addEventListener("click", handleShorten);
urlInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") handleShorten();
});

async function handleShorten() {
    const rawUrl = urlInput.value.trim();
    if (!rawUrl) {
        showToast("Please enter a valid URL.");
        return;
    }

    shortenBtn.disabled = true;
    shortenBtn.innerHTML = "Shortening...";

    try {
        const response = await fetch("/api/urls", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ url: rawUrl })
        });

        if (!response.ok) {
            throw new Error("Failed to shorten URL. Check format.");
        }

        const data = await response.json();
        const shortUrl = data.shortUrl;
        
        // Extract short code from URL (last path segment)
        const code = shortUrl.split("/").pop();
        currentShortCode = code;
        currentLongUrl = rawUrl;

        // Display results
        shortUrlDisplay.value = shortUrl;
        visitBtn.href = shortUrl;
        resultBox.classList.remove("hidden");

        // Set active target for benchmark
        activeShortCode.textContent = code;
        activeLongUrl.textContent = rawUrl;

        showToast("✅ URL Shortened! Ready for latency benchmark.");

    } catch (err) {
        showToast("❌ " + err.message);
    } finally {
        shortenBtn.disabled = false;
        shortenBtn.innerHTML = `
            <span>Shorten URL</span>
            <svg class="btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
        `;
    }
}

// Quick benchmark trigger from result card
quickBenchmarkBtn.addEventListener("click", () => {
    document.getElementById("benchmarkSection").scrollIntoView({ behavior: "smooth" });
    runFullBenchmark();
});

// Run Live Benchmark comparison
runBenchmarkBtn.addEventListener("click", runFullBenchmark);

async function runFullBenchmark() {
    if (isBenchmarking) return;

    // Ensure we have a short code, or auto-create one
    if (!currentShortCode) {
        await handleShorten();
        if (!currentShortCode) return;
    }

    isBenchmarking = true;
    runBenchmarkBtn.disabled = true;
    runBenchmarkIcon.textContent = "⏳";
    runBenchmarkText.textContent = "Benchmarking...";

    try {
        // Step 1: Call the server benchmark endpoint
        const response = await fetch(`/api/urls/benchmark/${currentShortCode}`);
        if (!response.ok) {
            throw new Error("Benchmark failed on server.");
        }

        const data = await response.json();
        
        // Extract timings
        const withoutMs = data.withoutRedis.latencyMs;
        const withMs = data.withRedis.latencyMs;
        const diff = data.latencyDifferenceMs;
        const reduction = data.latencyReductionPercent;
        const speedup = data.speedupFactor;

        // Update UI cards with animated count
        animateNumber(noRedisLatency, withoutMs);
        animateNumber(redisLatency, withMs);

        // Update Progress Bars
        const maxTime = Math.max(withoutMs, withMs, 10);
        const dbPercent = Math.min(100, Math.round((withoutMs / maxTime) * 100));
        const redisPercent = Math.min(100, Math.round((withMs / maxTime) * 100));

        noRedisBar.style.width = `${dbPercent}%`;
        redisBar.style.width = `${Math.max(5, redisPercent)}%`;

        redisStatusTag.textContent = "CACHE HIT";
        redisStatusTag.className = "spec-val status-tag tag-hit";

        // Update Insight banner
        speedupValue.textContent = speedup;
        reductionValue.textContent = `-${reduction}%`;
        savedMsValue.textContent = `${diff} ms`;
        insightSummary.innerHTML = `<strong>Performance Result:</strong> ${data.summary}`;

        // Append to history table
        addLogEntry({
            time: new Date().toLocaleTimeString(),
            code: currentShortCode,
            mode: "Comparison Benchmark",
            status: "HIT vs DIRECT",
            latency: `Redis: ${withMs}ms / DB: ${withoutMs}ms`,
            dbOps: "0 Reads (Redis) vs 1 Read (DB)"
        });

        showToast("⚡ Benchmark completed successfully!");

    } catch (err) {
        showToast("❌ " + err.message);
    } finally {
        isBenchmarking = false;
        runBenchmarkBtn.disabled = false;
        runBenchmarkIcon.textContent = "▶";
        runBenchmarkText.textContent = "Run Live Comparison";
    }
}

// 2. Test Direct MongoDB Only
testDbOnlyBtn.addEventListener("click", async () => {
    if (!currentShortCode) {
        showToast("Please shorten a URL first.");
        return;
    }

    testDbOnlyBtn.disabled = true;
    testDbOnlyBtn.textContent = "Testing...";

    const t0 = performance.now();
    try {
        const res = await fetch(`/api/urls/${currentShortCode}?useCache=false`);
        const roundTrip = Math.round(performance.now() - t0);
        const data = await res.json();
        const serverMs = data.timeTakenMs;

        animateNumber(noRedisLatency, serverMs);
        noRedisBar.style.width = "85%";

        addLogEntry({
            time: new Date().toLocaleTimeString(),
            code: currentShortCode,
            mode: "MongoDB Direct (No Cache)",
            status: "BYPASS",
            latency: `${serverMs} ms (RTT: ${roundTrip}ms)`,
            dbOps: "1 Read query executed"
        });

        showToast(`🍃 Direct DB query completed in ${serverMs} ms`);
    } catch (err) {
        showToast("❌ " + err.message);
    } finally {
        testDbOnlyBtn.disabled = false;
        testDbOnlyBtn.textContent = "Test Without Redis (Direct DB)";
    }
});

// 3. Test Redis Only
testRedisOnlyBtn.addEventListener("click", async () => {
    if (!currentShortCode) {
        showToast("Please shorten a URL first.");
        return;
    }

    testRedisOnlyBtn.disabled = true;
    testRedisOnlyBtn.textContent = "Testing...";

    const t0 = performance.now();
    try {
        const res = await fetch(`/api/urls/${currentShortCode}?useCache=true`);
        const roundTrip = Math.round(performance.now() - t0);
        const data = await res.json();
        const serverMs = data.timeTakenMs;
        const isHit = data.cacheHit;

        animateNumber(redisLatency, serverMs);
        redisBar.style.width = isHit ? "12%" : "90%";

        redisStatusTag.textContent = isHit ? "CACHE HIT" : "CACHE MISS";
        redisStatusTag.className = isHit ? "spec-val status-tag tag-hit" : "spec-val status-tag tag-miss";

        addLogEntry({
            time: new Date().toLocaleTimeString(),
            code: currentShortCode,
            mode: "Redis Cache-Aside",
            status: isHit ? "CACHE HIT" : "CACHE MISS",
            latency: `${serverMs} ms (RTT: ${roundTrip}ms)`,
            dbOps: isHit ? "0 Reads (Served from RAM)" : "1 Read + 1 Cache Write"
        });

        showToast(`⚡ Redis query completed in ${serverMs} ms (${isHit ? "CACHE HIT" : "CACHE MISS"})`);
    } catch (err) {
        showToast("❌ " + err.message);
    } finally {
        testRedisOnlyBtn.disabled = false;
        testRedisOnlyBtn.textContent = "Test With Redis (Cache Aside)";
    }
});

// 4. Evict Cache Key (Simulate Cache Miss)
evictCacheBtn.addEventListener("click", async () => {
    if (!currentShortCode) {
        showToast("Please shorten a URL first.");
        return;
    }

    evictCacheBtn.disabled = true;
    evictCacheBtn.textContent = "Evicting...";

    try {
        const res = await fetch(`/api/urls/cache/${currentShortCode}`, { method: "DELETE" });
        const data = await res.json();

        redisStatusTag.textContent = "EVICTED";
        redisStatusTag.className = "spec-val status-tag tag-miss";

        addLogEntry({
            time: new Date().toLocaleTimeString(),
            code: currentShortCode,
            mode: "Cache Eviction",
            status: "EVICTED",
            latency: "N/A",
            dbOps: "Key purged from Redis RAM"
        });

        showToast("🧹 Key evicted from Redis! Next request will trigger a Cache Miss.");
    } catch (err) {
        showToast("❌ " + err.message);
    } finally {
        evictCacheBtn.disabled = false;
        evictCacheBtn.textContent = "🧹 Evict Cache Key";
    }
});

// Copy to Clipboard
copyBtn.addEventListener("click", () => {
    if (!shortUrlDisplay.value) return;
    navigator.clipboard.writeText(shortUrlDisplay.value);
    copyText.textContent = "Copied!";
    setTimeout(() => { copyText.textContent = "Copy"; }, 2000);
});

// Log Table Helper
function addLogEntry(entry) {
    const emptyRow = logsTableBody.querySelector(".empty-row");
    if (emptyRow) emptyRow.remove();

    const tr = document.createElement("tr");
    
    let tagClass = "tag-hit";
    if (entry.status.includes("MISS") || entry.status === "EVICTED") tagClass = "tag-miss";
    else if (entry.status === "BYPASS") tagClass = "tag-bypass";

    tr.innerHTML = `
        <td style="font-family: var(--font-mono); color: var(--text-muted);">${entry.time}</td>
        <td><code class="code-badge">${entry.code}</code></td>
        <td><strong>${entry.mode}</strong></td>
        <td><span class="status-tag ${tagClass}">${entry.status}</span></td>
        <td style="font-family: var(--font-mono); font-weight: 700; color: #38bdf8;">${entry.latency}</td>
        <td style="color: var(--text-secondary);">${entry.dbOps}</td>
    `;

    logsTableBody.prepend(tr);
}

clearLogsBtn.addEventListener("click", () => {
    logsTableBody.innerHTML = `
        <tr class="empty-row">
            <td colspan="6">No tests run yet. Shorten a URL and run a comparison above.</td>
        </tr>
    `;
    showToast("Logs cleared.");
});

// Animated Counter
function animateNumber(element, target) {
    const start = 0;
    const duration = 600;
    const startTime = performance.now();

    function update(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const current = Math.round(start + (target - start) * easeOutQuad(progress));
        element.textContent = current;
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }
    requestAnimationFrame(update);
}

function easeOutQuad(t) {
    return t * (2 - t);
}

// Toast Helper
let toastTimeout;
function showToast(message) {
    clearTimeout(toastTimeout);
    toast.textContent = message;
    toast.classList.remove("hidden");
    toastTimeout = setTimeout(() => {
        toast.classList.add("hidden");
    }, 3200);
}