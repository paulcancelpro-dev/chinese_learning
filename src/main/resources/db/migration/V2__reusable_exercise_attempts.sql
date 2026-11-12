-- Existing generated exercises become the original content pool automatically.
-- Reused rows retain their own answers and revision events.
alter table exercise add column source_exercise_id uuid references exercise(id) on delete cascade;
create index exercise_reusable on exercise(type, difficulty) where source_exercise_id is null;
create index exercise_source on exercise(source_exercise_id);
