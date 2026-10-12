const shortenBtn = document.getElementById("shortenBtn");
const urlInput = document.getElementById("urlInput");
const result = document.getElementById("result");
const shortUrl = document.getElementById("shortUrl");
const copyBtn = document.getElementById("copyBtn");
console.log("Script Loaded");
shortenBtn.addEventListener("click", async () => {
    console.log("Button clicked");

    const url = urlInput.value.trim();

    if (url === "") {
        alert("Please enter a URL.");
        return;
    }

    try {

        const response = await fetch("/api/urls", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                url: url
            })
        });

        if (!response.ok) {
            throw new Error("Invalid URL");
        }

        const data = await response.json();

        shortUrl.value = data.shortUrl;

        result.classList.remove("hidden");

    } catch (error) {

        alert(error.message);

    }

});

copyBtn.addEventListener("click", () => {

    navigator.clipboard.writeText(shortUrl.value);

    copyBtn.innerText = "Copied!";

    setTimeout(() => {
        copyBtn.innerText = "Copy";
    }, 1500);

});