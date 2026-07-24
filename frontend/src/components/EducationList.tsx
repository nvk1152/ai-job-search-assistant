/**
 * Education editor: add/edit/remove education entries (institution, degree, field, dates).
 * Integrates with ProfileEditor parent state.
 */
import type { Education } from "../types/profile";

interface Props {
  education: Education[];
  onChange: (education: Education[]) => void;
}

export default function EducationList({ education, onChange }: Props) {
  const update = (index: number, patch: Partial<Education>) => {
    onChange(education.map((e, i) => (i === index ? { ...e, ...patch } : e)));
  };
  const remove = (index: number) => onChange(education.filter((_, i) => i !== index));
  const add = () =>
    onChange([
      ...education,
      { id: null, profileId: null, institution: "", degree: "", field: "", startDate: null, endDate: null },
    ]);

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold">Education</h2>
      {education.map((entry, i) => (
        <div key={i} className="flex gap-2">
          <input
            className="flex-1 rounded border px-2 py-1"
            placeholder="Institution"
            value={entry.institution}
            onChange={(e) => update(i, { institution: e.target.value })}
          />
          <input
            className="flex-1 rounded border px-2 py-1"
            placeholder="Degree"
            value={entry.degree ?? ""}
            onChange={(e) => update(i, { degree: e.target.value })}
          />
          <input
            className="flex-1 rounded border px-2 py-1"
            placeholder="Field"
            value={entry.field ?? ""}
            onChange={(e) => update(i, { field: e.target.value })}
          />
          <button type="button" className="text-red-600" onClick={() => remove(i)}>
            Remove
          </button>
        </div>
      ))}
      <button type="button" className="rounded bg-slate-200 px-3 py-1 dark:bg-slate-700" onClick={add}>
        + Add education
      </button>
    </section>
  );
}
