const API_BASE = "https://ea.electrolyte.dev/api";

export async function uploadFile(file, graderType) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("graderType", graderType);

  const response = await fetch(`${API_BASE}/jobs/upload`, {
    method: "POST",
    body: formData
  });

  const message = await response.json();

  if (!response.ok) throw new Error(message.message);
  return message;
}
