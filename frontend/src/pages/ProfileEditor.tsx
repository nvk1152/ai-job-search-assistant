import { useEffect, useState } from "react";
import { getProfile, importResume, saveProfile } from "../api/profileApi";
import { emptyProfileView, type ProfileView } from "../types/profile";
import ExperienceList from "../components/ExperienceList";
import SkillList from "../components/SkillList";
import EducationList from "../components/EducationList";
import ResumeUpload from "../components/ResumeUpload";

/**
 * Profile editor page: fetch, edit, and save user profile (headline, summary, experiences, skills, education).
 * Also handles résumé import, which extracts data server-side and merges into the profile.
 */
export default function ProfileEditor() {
  const [view, setView] = useState<ProfileView>(emptyProfileView());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getProfile()
      .then(setView)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setSaved(false);
    try {
      const toSave: ProfileView = {
        ...view,
        experiences: view.experiences.map((e) => ({
          ...e,
          bullets: e.bullets.filter((b) => b.trim() !== ""),
        })),
      };
      const updated = await saveProfile(toSave);
      setView(updated);
      setSaved(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Save failed");
    } finally {
      setSaving(false);
    }
  };

  const handleImport = async (file: File) => {
    const draft = await importResume(file);
    setView(draft);
    setSaved(false);
  };

  if (loading) {
    return <p>Loading profile…</p>;
  }

  return (
    <div className="space-y-6">
      <ResumeUpload onImported={handleImport} />

      <section className="space-y-2">
        <input
          className="w-full rounded border px-2 py-1 text-lg font-medium"
          placeholder="Headline"
          value={view.profile.headline ?? ""}
          onChange={(e) => setView({ ...view, profile: { ...view.profile, headline: e.target.value } })}
        />
        <textarea
          className="w-full rounded border px-2 py-1"
          placeholder="Summary"
          value={view.profile.summary ?? ""}
          onChange={(e) => setView({ ...view, profile: { ...view.profile, summary: e.target.value } })}
        />
      </section>

      <ExperienceList
        experiences={view.experiences}
        onChange={(experiences) => setView({ ...view, experiences })}
      />
      <SkillList skills={view.skills} onChange={(skills) => setView({ ...view, skills })} />
      <EducationList education={view.education} onChange={(education) => setView({ ...view, education })} />

      <div className="flex items-center gap-3">
        <button
          type="button"
          className="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
          disabled={saving}
          onClick={handleSave}
        >
          {saving ? "Saving…" : "Save profile"}
        </button>
        {saved && <span className="text-sm text-green-600">Saved.</span>}
        {error && <span className="text-sm text-red-600">{error}</span>}
      </div>
    </div>
  );
}
