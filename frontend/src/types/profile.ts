/**
 * ProfileView type: combines Profile (headline, summary) with lists of experiences, skills,
 * and education. Used throughout API calls and component state.
 */
export interface Profile {
  id: string | null;
  userId: string | null;
  headline: string | null;
  summary: string | null;
}

export interface Experience {
  id: string | null;
  profileId: string | null;
  company: string;
  title: string;
  startDate: string | null;
  endDate: string | null;
  bullets: string[];
}

export interface Skill {
  id: string | null;
  profileId: string | null;
  name: string;
  level: string | null;
}

export interface Education {
  id: string | null;
  profileId: string | null;
  institution: string;
  degree: string | null;
  field: string | null;
  startDate: string | null;
  endDate: string | null;
}

export interface ProfileView {
  profile: Profile;
  experiences: Experience[];
  skills: Skill[];
  education: Education[];
}

export const emptyProfileView = (): ProfileView => ({
  profile: { id: null, userId: null, headline: "", summary: "" },
  experiences: [],
  skills: [],
  education: [],
});
