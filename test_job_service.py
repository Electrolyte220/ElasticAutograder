from job_service import create_job

def test_create_job_sets_status_to_queued():
    job = create_job("submission.zip")
    assert job["status"] == "queued"
    assert job["original_filename"] == "submission.zip"
