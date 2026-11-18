-- Ejecutar UNA vez en Supabase: SQL Editor > New query > pegar y Run
create table if not exists pagos_yape (
  id bigint generated always as identity primary key,
  monto numeric not null,
  creado timestamptz not null default now(),
  usado boolean not null default false
);

alter table pagos_yape enable row level security;

create policy "pagos_yape_insertar" on pagos_yape for insert to anon with check (true);
create policy "pagos_yape_leer" on pagos_yape for select to anon using (true);
create policy "pagos_yape_actualizar" on pagos_yape for update to anon using (true);

grant select, insert, update on pagos_yape to anon;
grant usage, select on sequence pagos_yape_id_seq to anon;
