package com.example.pitchperfect.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pitchperfect.R;
import com.example.pitchperfect.models.PitchDeck;
import com.example.pitchperfect.ui.practice.PracticeActivity;

import java.util.List;
import java.util.Locale;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {

    private Context context;
    private List<PitchDeck> decks;

    public DeckAdapter(Context context, List<PitchDeck> decks) {
        this.context = context;
        this.decks = decks;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        PitchDeck deck = decks.get(position);
        holder.tvTitle.setText(deck.getTitle());
        holder.tvSlides.setText(String.format(Locale.getDefault(), "%d slides", deck.getTotalSlides()));
        holder.tvStatus.setText(deck.getStatus());

        // Color status
        int color = "completed".equals(deck.getStatus()) ? 0xFF28A745 : 0xFFFFC107;
        holder.tvStatus.setTextColor(color);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PracticeActivity.class);
            intent.putExtra("deck_id", deck.getId());
            intent.putExtra("deck_title", deck.getTitle());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return decks.size(); }

    static class DeckViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSlides, tvStatus;

        DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvDeckTitle);
            tvSlides = itemView.findViewById(R.id.tvSlideCount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
