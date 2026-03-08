export default function JobsTable({ jobs }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Assignment</th>
          <th>Filename</th>
          <th>Status</th>
          <th>Created</th>
          <th>Score</th>
          <th>Tests</th>
        </tr>
      </thead>
      <tbody>
        {jobs.map((job) => {
          const statusClass = `status status-${(job.status || "").toLowerCase()}`;

          return (
            <tr key={job.id}>
              <td>{job.id}</td>
              <td>{job.assignmentId ?? ""}</td>
              <td>{job.originalFilename ?? ""}</td>
              <td className={statusClass}>{job.status ?? ""}</td>
              <td>{job.createdAt ?? ""}</td>
              <td>{job.score ?? ""}</td>
              <td>
                {job.testsPassed ?? ""} / {job.testsTotal ?? ""}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}