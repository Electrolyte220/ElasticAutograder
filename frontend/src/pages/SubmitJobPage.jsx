import { Link } from "react-router-dom";
import { useState } from 'react';
import { uploadFile } from "../api/upload_file";

export default function SubmitJobPage() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState("");

  const handleFileChange = e => {
    setFile(e.target.files[0]);
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setStatus("Please select a file to upload.");
      return;
    }
    try {
      setStatus("Uploading...");
      const message = await uploadFile(file);
      setStatus(message);
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
