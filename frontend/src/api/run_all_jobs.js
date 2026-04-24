const API_BASE = "http://localhost:8080/api";

export async function runAllJobs(minId, maxId) {
  const formData = new FormData();
  formData.append("minId", minId);
  formData.append("maxId", maxId);
  const response = await fetch(`${API_BASE}/jobs/run-all`, {
    method: "POST",
    body: formData
  })

  if (!response.ok) throw new Error("Unable to run jobs");
  return response;
}
