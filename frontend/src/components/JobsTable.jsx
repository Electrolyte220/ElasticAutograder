export default function JobsTable({ jobs }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>Grader Type</th>
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
          const formatedDate = new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: 'numeric',
            second: 'numeric'
          }).format(new Date(job.createdAt));


          return (
            <tr key={job.id}>
              <td>{job.id}</td>
              <td>{job.graderType ?? ""}</td>
              <td>{job.originalFilename ?? ""}</td>
              <td className={statusClass}>{job.status ?? ""}</td>
              <td className={formatedDate}>{formatedDate ?? ""}</td>
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
