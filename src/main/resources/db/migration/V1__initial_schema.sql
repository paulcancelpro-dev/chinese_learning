create table word (
 id uuid primary key,
 hanzi varchar(100) not null check (length(trim(hanzi)) > 0),
 pinyin varchar(200) not null check (length(trim(pinyin)) > 0),
 unique(hanzi,pinyin)
);
create table word_translation (
 word_id uuid not null references word(id) on delete cascade,
 translation varchar(300) not null,
 primary key(word_id,translation)
);
create table progress (
 id uuid primary key,
 word_id uuid not null references word(id) on delete cascade,
 skill varchar(255) not null check (skill in ('HANZI_RECOGNITION','HANZI_TO_PINYIN','HANZI_TO_FRENCH','WRITING')),
 level integer check(level between 0 and 5),
 successes bigint not null default 0 check(successes >= 0),
 errors bigint not null default 0 check(errors >= 0),
 streak integer not null default 0 check(streak >= 0),
 last_review timestamptz,
 next_review timestamptz,
 unique(word_id,skill)
);
create index progress_due on progress(next_review);
create table training_session (
 id uuid primary key,
 created_at timestamptz not null,
 generator varchar(255) not null
);
create table exercise (
 id uuid primary key,
 session_id uuid not null references training_session(id),
 word_id uuid not null references word(id) on delete cascade,
 type varchar(255) not null check(type in ('HANZI_TO_FRENCH','HANZI_TO_PINYIN','PINYIN_TO_HANZI','MULTIPLE_CHOICE','SENTENCE_TRANSLATION','FILL_IN_THE_BLANK')),
 question varchar(1000) not null,
 difficulty integer not null check(difficulty between 1 and 5),
 explanation varchar(2000) not null,
 submitted_answer varchar(2000),
 correct boolean,
 answered_at timestamptz,
 check ((answered_at is null and correct is null and submitted_answer is null) or (answered_at is not null and correct is not null and submitted_answer is not null))
);
create table exercise_choice (
 exercise_id uuid not null references exercise(id) on delete cascade,
 position integer not null,
 value varchar(300) not null,
 primary key(exercise_id,position)
);
create table exercise_answer (
 exercise_id uuid not null references exercise(id) on delete cascade,
 value varchar(2000) not null
);
create table exercise_new_vocabulary (
 exercise_id uuid not null references exercise(id) on delete cascade,
 value varchar(100) not null
);
create table review_event (
 id uuid primary key,
 word_id uuid not null references word(id) on delete cascade,
 skill varchar(255) not null check (skill in ('HANZI_RECOGNITION','HANZI_TO_PINYIN','HANZI_TO_FRENCH','WRITING')),
 correct boolean not null,
 reviewed_at timestamptz not null,
 answer varchar(2000) not null
);
create index review_word on review_event(word_id,skill,reviewed_at);
create index exercise_history on exercise(answered_at);
