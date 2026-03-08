import { Link, useNavigate } from "react-router-dom";
import { useState } from 'react';
import { uploadFile } from "../api/upload_file";
import { runJob } from "../api/run_job";
import { updateDB } from "../api/update_db";

export default function SubmitJobPage() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState("");
  const navigate = useNavigate();

  const handleFileChange = e => {
    setFile(e.target.files[0]);
  }

  const handleSubmit = async () => {
    if (!file) {
      setStatus("Please select a file to upload.");
      return;
    }
    try {
      setStatus("Uploading...");
      const message = await uploadFile(file);
      setStatus(message.message);
      navigate("/");
      const jobResponse = await runJob(message.id, file.name);
      const jobResults = await jobResponse.json();
      await updateDB(message.id, jobResults);
    } catch (err) {
      setStatus(err.message);
    }
  }

  return (
    <div className="jobs-page">
      <div className="jobs-board-shell">
        <div className="top-bar">
          <h1 className="page-title">Submit Job</h1>
          <Link to="/" className="button nav-button">
            Back to Jobs
          </Link>
        </div>

        <div className="card">
          <div className="form-group">
            <label className="label">Assignment ID</label>
            <input className="input" type="text" placeholder="e.g. hw1" />
          </div>

          <div className="form-group">
            <label className="label">Grader Image</label>
            <input className="input" type="text" placeholder="e.g. python-grader:latest" />
          </div>

          <div className="form-group">
            <label className="label">Upload Submission</label>
            <input className="input" type="file" onChange={handleFileChange}/>
          </div>

          <button className="button" onClick={handleSubmit}>
            Submit Job
          </button>
          {status && <p>{status}</p>}
        </div>
      </div>
    </div>
  );
}
