const API_BASE = "http://localhost:8080/api";

export async function fetchRecentJobs() {
  const res = await fetch(`${API_BASE}/jobs/recent`);

  if (!res.ok) {
    throw new Error(`Failed to fetch jobs: ${res.status}`);
  }

  return res.json();
}

export async function fetchJobsInRange(minId, maxId) {
  const res = await fetch(`${API_BASE}/jobs/multi-submission?from=${minId}&to=${maxId}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch jobs in range ${minId}-${maxId}: ${res.status}`);
    }
  return res.json();
}
