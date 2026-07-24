/**
 * Work history editor: add/edit/remove experience entries (company, title, dates, bullet points).
 * Integrates with ProfileEditor parent state.
 */
import type { Experience } from "../types/profile";

interface Props {
  experiences: Experience[];
  onChange: (experiences: Experience[]) => void;
}

export default function ExperienceList({ experiences, onChange }: Props) {
  const update = (index: number, patch: Partial<Experience>) => {
    onChange(experiences.map((e, i) => (i === index ? { ...e, ...patch } : e)));
  };
  const remove = (index: number) => onChange(experiences.filter((_, i) => i !== index));
  const add = () =>
    onChange([
      ...experiences,
      { id: null, profileId: null, company: "", title: "", startDate: null, endDate: null, bullets: [] },
    ]);

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold">Experience</h2>
      {experiences.map((exp, i) => (
        <div key={i} className="space-y-2 rounded border border-slate-300 p-3 dark:border-slate-700">
          <div className="flex gap-2">
            <input
              className="flex-1 rounded border px-2 py-1"
              placeholder="Company"
              value={exp.company}
              onChange={(e) => update(i, { company: e.target.value })}
            />
            <input
              className="flex-1 rounded border px-2 py-1"
              placeholder="Title"
              value={exp.title}
              onChange={(e) => update(i, { title: e.target.value })}
            />
            <button type="button" className="text-red-600" onClick={() => remove(i)}>
              Remove
            </button>
          </div>
          <div className="flex gap-2">
            <input
              type="date"
              className="rounded border px-2 py-1"
              value={exp.startDate ?? ""}
              onChange={(e) => update(i, { startDate: e.target.value || null })}
            />
            <input
              type="date"
              className="rounded border px-2 py-1"
              value={exp.endDate ?? ""}
              onChange={(e) => update(i, { endDate: e.target.value || null })}
            />
          </div>
          <textarea
            className="w-full rounded border px-2 py-1"
            placeholder="Bullets, one per line"
            value={exp.bullets.join("\n")}
            onChange={(e) => update(i, { bullets: e.target.value.split("\n") })}
          />
        </div>
      ))}
      <button type="button" className="rounded bg-slate-200 px-3 py-1 dark:bg-slate-700" onClick={add}>
        + Add experience
      </button>
    </section>
  );
}
