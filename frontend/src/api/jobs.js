const API_BASE = "https://ea.electrolyte.dev/api";

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

export async function fetchJobById(jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}`);

  if (!res.ok) {
    throw new Error(`Failed to fetch job ${jobId}: ${res.status}`);
  }

  return res.json();
}
