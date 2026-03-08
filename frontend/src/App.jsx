import { Routes, Route } from "react-router-dom";
import JobsBoard from "./pages/JobsBoard";
import SubmitJobPage from "./pages/SubmitJobPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<JobsBoard />} />
      <Route path="/submit" element={<SubmitJobPage />} />
    </Routes>
  );
}