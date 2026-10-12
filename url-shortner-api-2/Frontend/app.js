const longUrlInput = document.getElementById("longUrl");
const shortenBtn = document.getElementById("shortenBtn");
const result = document.getElementById("result");
const shortUrl = document.getElementById("shortUrl");
const copyBtn = document.getElementById("copyBtn");

shortenBtn.addEventListener("click", async () => {


const longUrl = longUrlInput.value.trim();

if (longUrl === "") {
    alert("Please enter a URL");
    return;
}

try {

    const response = await fetch("http://localhost:8081/shorten", {

        method: "POST",

        headers: {
            "Content-Type": "application/json"
        },

        body: JSON.stringify({
            longUrl: longUrl
        })
    });


    if (!response.ok) {
        throw new Error("Failed to shorten URL");
    }


    const shortCode = await response.text();


    const fullShortUrl =
        "http://localhost:8081/" + shortCode;


    shortUrl.href = fullShortUrl;
    shortUrl.textContent = fullShortUrl;

    result.style.display = "block";

} catch (error) {

    console.error(error);

    alert("Something went wrong. Make sure the backend is running.");

}


});

copyBtn.addEventListener("click", async () => {


await navigator.clipboard.writeText(shortUrl.href);

copyBtn.textContent = "Copied!";

setTimeout(() => {
    copyBtn.textContent = "Copy";
}, 1500);


});
