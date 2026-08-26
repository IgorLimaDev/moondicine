-- Add read access for ai_explanations so the mobile app can fetch explanations.

drop policy if exists "Anyone can read ai explanations" on public.ai_explanations;
create policy "Anyone can read ai explanations"
    on public.ai_explanations for select to anon, authenticated using (true);

-- Also enable read access for user_stats (syncs streak data).
drop policy if exists "Anyone can read user stats" on public.user_stats;
create policy "Anyone can read user stats"
    on public.user_stats for select to anon, authenticated using (true);
