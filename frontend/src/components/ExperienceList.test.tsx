import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ExperienceList from "./ExperienceList";
import type { Experience } from "../types/profile";

describe("ExperienceList", () => {
  it("adds a blank experience row", () => {
    const onChange = vi.fn();
    render(<ExperienceList experiences={[]} onChange={onChange} />);

    fireEvent.click(screen.getByText("+ Add experience"));

    expect(onChange).toHaveBeenCalledWith([
      { id: null, profileId: null, company: "", title: "", startDate: null, endDate: null, bullets: [] },
    ]);
  });

  it("removes an experience row", () => {
    const experiences: Experience[] = [
      { id: "1", profileId: "p1", company: "Acme", title: "Engineer", startDate: null, endDate: null, bullets: [] },
    ];
    const onChange = vi.fn();
    render(<ExperienceList experiences={experiences} onChange={onChange} />);

    fireEvent.click(screen.getByText("Remove"));

    expect(onChange).toHaveBeenCalledWith([]);
  });

  it("edits the company field without touching other fields", () => {
    const experiences: Experience[] = [
      { id: "1", profileId: "p1", company: "", title: "", startDate: null, endDate: null, bullets: [] },
    ];
    const onChange = vi.fn();
    render(<ExperienceList experiences={experiences} onChange={onChange} />);

    fireEvent.change(screen.getByPlaceholderText("Company"), { target: { value: "Acme" } });

    expect(onChange).toHaveBeenCalledWith([{ ...experiences[0], company: "Acme" }]);
  });
});
