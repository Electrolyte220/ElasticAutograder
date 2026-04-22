const API_BASE = "http://localhost:8080/api";

export async function fetchRecentJobs() {
  const res = await fetch(`${API_BASE}/jobs/recent`);

  if (!res.ok) {
    throw new Error(`Failed to fetch jobs: ${res.status}`);
  }

  return res.json();
}

export async function fetchGraders() {
  const res = await fetch(`${API_BASE}/graders`);

  if (!res.ok) {
    throw new Error(`Failed to fetch graders: ${res.status}`);
  }

  return res.json();
}