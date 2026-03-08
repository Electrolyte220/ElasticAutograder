-- main table creation sql command 
CREATE TABLE IF NOT EXISTS jobs (
  id BIGSERIAL PRIMARY KEY,

  assignment_id TEXT,
  original_filename TEXT NOT NULL,
  submission_path TEXT,
  grader_image TEXT,

  status TEXT NOT NULL CHECK (
    status IN ('PENDING', 'QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')
  ),

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,

  score NUMERIC,
  tests_passed INT,
  tests_total INT,

  error_message TEXT,
  result_json JSONB,

  k8s_job_name TEXT
);

-- indexes implemented to speed up querying (these are the most likely to be queried for so introducing indexes helps performance :) )
CREATE INDEX IF NOT EXISTS idx_jobs_status
  ON jobs(status);

CREATE INDEX IF NOT EXISTS idx_jobs_assignment_id
  ON jobs(assignment_id);

CREATE INDEX IF NOT EXISTS idx_jobs_created_at
  ON jobs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_jobs_k8s_job_name
  ON jobs(k8s_job_name);

-- additional helper for whenever a query is updated
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_jobs_updated_at ON jobs;

CREATE TRIGGER trg_jobs_updated_at
BEFORE UPDATE ON jobs
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();