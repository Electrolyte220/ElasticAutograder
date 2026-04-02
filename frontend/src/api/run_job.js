const API_BASE = "https://elastic-autograder-backend.onrender.com/api";

export async function runJob(jobId, fileName) {
  const response = await fetch(`${API_BASE}/jobs/run/${jobId}`, {
    method: "POST",
    body: fileName
  })

  if (!response.ok) throw new Error("Unable to run job");
  return response;
}
