import { Routes, Route } from "react-router-dom";
import JobsBoard from "./pages/JobsBoard";
import SubmitJobPage from "./pages/SubmitJobPage";
import RunJob from "./pages/RunJobPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<JobsBoard />} />
      <Route path="/submit" element={<SubmitJobPage />} />
      <Route path="/progress/:id" element={<RunJob />}/>
    </Routes>
  );
}
