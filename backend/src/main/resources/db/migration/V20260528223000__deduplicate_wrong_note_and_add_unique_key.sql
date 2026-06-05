DELETE wn
FROM wrong_note wn
JOIN wrong_note keep
  ON keep.user_id = wn.user_id
 AND keep.problem_id = wn.problem_id
WHERE keep.id <> wn.id
  AND (
    keep.updated_at > wn.updated_at
    OR (keep.updated_at = wn.updated_at AND keep.id > wn.id)
  );

ALTER TABLE wrong_note
    ADD CONSTRAINT uq_wrong_note_user_problem UNIQUE (user_id, problem_id);
