const messagesEl = document.getElementById("messages");
const formEl = document.getElementById("chat-form");
const inputEl = document.getElementById("prompt-input");
const sendBtn = document.getElementById("send-btn");
const errorBanner = document.getElementById("error-banner");
const errorMessage = document.getElementById("error-message");
const dismissError = document.getElementById("dismiss-error");
const newChatBtn = document.getElementById("new-chat-btn");
const welcomeEl = document.getElementById("welcome");
const charCount = document.getElementById("char-count");

let history = [];
let isLoading = false;

function appendMessage(role, content) {
  welcomeEl.hidden = true;

  const row = document.createElement("div");
  row.className = `message-row ${role}`;

  if (role === "assistant" || role === "loading") {
    const avatar = document.createElement("div");
    avatar.className = "avatar";
    avatar.textContent = "S";
    avatar.setAttribute("aria-hidden", "true");
    row.appendChild(avatar);
  }

  const bubble = document.createElement("div");
  bubble.className = role === "loading" ? "message typing" : "message";

  if (role === "loading") {
    bubble.setAttribute("aria-label", "Waiting for reply");
    for (let i = 0; i < 3; i += 1) {
      bubble.appendChild(document.createElement("span"));
    }
  } else {
    bubble.textContent = content;
  }

  row.appendChild(bubble);
  messagesEl.appendChild(row);
  messagesEl.scrollTop = messagesEl.scrollHeight;
  return row;
}

function showError(message) {
  errorMessage.textContent = message;
  errorBanner.hidden = false;
}

function clearError() {
  errorBanner.hidden = true;
  errorMessage.textContent = "";
}

function updateComposer() {
  charCount.textContent = `${inputEl.value.length} / 4000`;
  sendBtn.disabled = isLoading || !inputEl.value.trim();
  newChatBtn.disabled = isLoading;
  inputEl.style.height = "auto";
  inputEl.style.height = `${Math.min(inputEl.scrollHeight, 140)}px`;
}

function setLoading(loading) {
  isLoading = loading;
  inputEl.disabled = loading;
  updateComposer();
}

async function readResponse(response) {
  const text = await response.text();
  if (!text) return {};

  try {
    return JSON.parse(text);
  } catch {
    if (!response.ok) {
      return { message: "The server returned an unexpected error." };
    }
    throw new Error("The server returned an invalid response.");
  }
}

formEl.addEventListener("submit", async (e) => {
  e.preventDefault();
  const prompt = inputEl.value.trim();
  if (!prompt || isLoading) return;

  clearError();
  const requestHistory = history.slice(-40);
  history.push({ role: "user", content: prompt });
  history = history.slice(-40);

  const userRow = appendMessage("user", prompt);
  inputEl.value = "";
  updateComposer();
  setLoading(true);

  const loadingRow = appendMessage("loading");

  try {
    const response = await fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ prompt, history: requestHistory }),
    });

    const data = await readResponse(response);

    if (!response.ok) {
      showError(data.message || "Something went wrong. Please try again.");
      userRow.classList.add("failed");
      return;
    }

    if (typeof data.reply !== "string" || !data.reply.trim()) {
      throw new Error("The service returned an empty response.");
    }

    appendMessage("assistant", data.reply);
    history.push({ role: "assistant", content: data.reply });
    history = history.slice(-40);

  } catch (err) {
    userRow.classList.add("failed");
    const message = err instanceof TypeError
      ? "Could not reach the server. Check your connection and try again."
      : err.message || "Something went wrong. Please try again.";
    showError(message);
  } finally {
    loadingRow.remove();
    setLoading(false);
    inputEl.focus();
  }
});

inputEl.addEventListener("input", updateComposer);

inputEl.addEventListener("keydown", (event) => {
  if (event.key === "Enter" && !event.shiftKey && !event.isComposing) {
    event.preventDefault();
    formEl.requestSubmit();
  }
});

document.querySelectorAll("[data-prompt]").forEach((button) => {
  button.addEventListener("click", () => {
    inputEl.value = button.dataset.prompt;
    updateComposer();
    inputEl.focus();
  });
});

dismissError.addEventListener("click", clearError);

newChatBtn.addEventListener("click", () => {
  history = [];
  clearError();
  messagesEl.querySelectorAll(".message-row").forEach((message) => message.remove());
  welcomeEl.hidden = false;
  inputEl.value = "";
  updateComposer();
  inputEl.focus();
});

updateComposer();
