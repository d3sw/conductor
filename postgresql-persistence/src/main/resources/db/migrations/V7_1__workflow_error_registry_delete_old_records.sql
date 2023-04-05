DELETE FROM workflow_error_registry WHERE workflow_id NOT IN
    (SELECT workflow_id FROM workflow WHERE end_time < (current_date  - interval '60 days'))
    and end_time < (current_date  - interval '60 days');