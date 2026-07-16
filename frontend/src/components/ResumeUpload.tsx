/**
 * Résumé upload widget: accepts PDF/DOCX, sends to backend for text extraction and LLM parsing.
 * Calls parent's onImported callback with extracted data to populate the profile editor.
 */
import { useRef, useState } from "react";

interface Props {
  onImported: (file: File) => Promise<void>;
}

export default function ResumeUpload({ onImported }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File) => {
    setBusy(true);
    setError(null);
    try {
      await onImported(file);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Import failed");
    } finally {
      setBusy(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  return (
    <section className="space-y-2 rounded border border-dashed border-slate-400 p-4">
      <h2 className="text-lg font-semibold">Import résumé</h2>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        Upload a PDF or DOCX résumé. The extracted draft appears below for you to review before saving.
      </p>
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.docx"
        disabled={busy}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleFile(file);
        }}
      />
      {busy && <p className="text-sm text-slate-500">Extracting…</p>}
      {error && <p className="text-sm text-red-600">{error}</p>}
    </section>
  );
}
