/**
 * Skills editor: add/edit/remove skill entries (name, optional level like "Expert" or "Intermediate").
 * Integrates with ProfileEditor parent state.
 */
import type { Skill } from "../types/profile";

interface Props {
  skills: Skill[];
  onChange: (skills: Skill[]) => void;
}

export default function SkillList({ skills, onChange }: Props) {
  const update = (index: number, patch: Partial<Skill>) => {
    onChange(skills.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  };
  const remove = (index: number) => onChange(skills.filter((_, i) => i !== index));
  const add = () => onChange([...skills, { id: null, profileId: null, name: "", level: "" }]);

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold">Skills</h2>
      {skills.map((skill, i) => (
        <div key={i} className="flex gap-2">
          <input
            className="flex-1 rounded border px-2 py-1"
            placeholder="Skill"
            value={skill.name}
            onChange={(e) => update(i, { name: e.target.value })}
          />
          <input
            className="w-32 rounded border px-2 py-1"
            placeholder="Level"
            value={skill.level ?? ""}
            onChange={(e) => update(i, { level: e.target.value })}
          />
          <button type="button" className="text-red-600" onClick={() => remove(i)}>
            Remove
          </button>
        </div>
      ))}
      <button type="button" className="rounded bg-slate-200 px-3 py-1 dark:bg-slate-700" onClick={add}>
        + Add skill
      </button>
    </section>
  );
}
