const API_BASE = "http://localhost:8080/api";

export async function updateDB(jobId, jobResults) {
  const response = await fetch(`${API_BASE}/jobs/${jobId}/callback`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(jobResults)
  })

  if (!response.ok) throw new Error("Unable to save job results to db.");
  return response;
}
