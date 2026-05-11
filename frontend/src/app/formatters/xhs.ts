const CIRCLED_NUMBERS = ["①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨"];

export interface XhsFormattedContent {
  title: string;
  body: string;
  text: string;
}

export function formatXhsContent(title: string, markdown: string): XhsFormattedContent {
  const safeTitle = normalizeInline(title).replace(/^#+\s*/, "").trim() || "未命名笔记";
  const body = markdownToXhsPlainText(stripDuplicatedTitle(markdown, safeTitle));
  return {
    title: safeTitle,
    body,
    text: [safeTitle, body].filter(Boolean).join("\n\n").trim(),
  };
}

export function stripDuplicatedTitle(content: string, title: string) {
  const escaped = title.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return content.replace(new RegExp(`^#\\s+${escaped}\\s*\\n+`, "i"), "").trim();
}

function markdownToXhsPlainText(markdown: string) {
  const output: string[] = [];
  const lines = markdown.replace(/\r\n?/g, "\n").split("\n");
  let previousBlank = true;

  const pushBlank = () => {
    if (!previousBlank && output.length > 0) {
      output.push("");
      previousBlank = true;
    }
  };

  const pushLine = (line: string) => {
    const value = line.trim();
    if (!value) {
      pushBlank();
      return;
    }
    output.push(value);
    previousBlank = false;
  };

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      pushBlank();
      continue;
    }

    if (/^!\[[^\]]*]\([^)]+\)\s*$/.test(line)) {
      pushBlank();
      continue;
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      pushBlank();
      pushLine(formatHeading(heading[1].length, heading[2]));
      pushBlank();
      continue;
    }

    const quote = line.match(/^>\s?(.+)$/);
    if (quote) {
      pushLine(`▎${normalizeInline(quote[1])}`);
      continue;
    }

    const unordered = line.match(/^[-*+]\s+(.+)$/);
    if (unordered) {
      pushLine(`• ${normalizeInline(unordered[1])}`);
      continue;
    }

    const ordered = line.match(/^(\d+)[.)、]\s+(.+)$/);
    if (ordered) {
      const index = Number.parseInt(ordered[1], 10);
      const marker = index >= 1 && index <= CIRCLED_NUMBERS.length ? CIRCLED_NUMBERS[index - 1] : `${index}.`;
      pushLine(`${marker} ${normalizeInline(ordered[2])}`);
      continue;
    }

    pushLine(normalizeInline(line));
  }

  return output.join("\n").replace(/\n{3,}/g, "\n\n").trim();
}

function formatHeading(level: number, text: string) {
  const clean = normalizeInline(text);
  if (level <= 1) return clean;
  if (level === 2) return `📌 ${clean}`;
  return `—— ${clean} ——`;
}

function normalizeInline(text: string) {
  return text
    .replace(/!\[([^\]]*)]\([^)]+\)/g, "$1")
    .replace(/\[([^\]]+)]\([^)]+\)/g, "$1")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/__([^_]+)__/g, "$1")
    .replace(/\*([^*\n]+)\*/g, "$1")
    .replace(/_([^_\n]+)_/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/~~([^~]+)~~/g, "$1")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/[ \t]{2,}/g, " ")
    .trim();
}
