package com.example.pitchperfect.ui.practice;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pitchperfect.R;
import com.example.pitchperfect.models.PracticeSession;
import com.example.pitchperfect.ui.feedback.FeedbackActivity;

import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private Context context;
    private List<PracticeSession> sessions;

    public SessionAdapter(Context context, List<PracticeSession> sessions) {
        this.context = context;
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        PracticeSession session = sessions.get(position);
        holder.tvSessionNumber.setText(String.format(Locale.getDefault(), "Session #%d", session.getSessionNumber()));
        holder.tvSessionDate.setText(session.getCreatedAt() != null ?
                session.getCreatedAt().substring(0, 10) : "");
        holder.tvSessionScore.setText(String.valueOf((int) session.getOverallScore()));

        holder.itemView.setOnClickListener(v -> {
            if ("completed".equals(session.getStatus())) {
                Intent intent = new Intent(context, FeedbackActivity.class);
                intent.putExtra("session_id", session.getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() { return sessions.size(); }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView tvSessionNumber, tvSessionDate, tvSessionScore;

        SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionNumber = itemView.findViewById(R.id.tvSessionNumber);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            tvSessionScore = itemView.findViewById(R.id.tvSessionScore);
        }
    }
}
