package com.project.enginuity.interaction.service;

import com.project.enginuity.interaction.entities.ReelCommentEntity;
import com.project.enginuity.interaction.entities.ReelLikeEntity;
import com.project.enginuity.interaction.entities.ReelSaveEntity;
import com.project.enginuity.interaction.io.CommentResponse;
import com.project.enginuity.interaction.io.LikeResponse;
import com.project.enginuity.interaction.io.SaveResponse;
import com.project.enginuity.interaction.repository.ReelCommentRepo;
import com.project.enginuity.interaction.repository.ReelLiKeRepo;
import com.project.enginuity.interaction.repository.ReelSaveRepo;
import com.project.enginuity.post.entity.ReelEntity;
import com.project.enginuity.post.io.ReelResponse;
import com.project.enginuity.post.repository.ReelRepo;
import com.project.enginuity.profile.model.UserEntity;
import com.project.enginuity.profile.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InteractionService {
    @Autowired
    private ReelLiKeRepo reelLiKeRepo;
    @Autowired
    private ReelCommentRepo reelCommentRepo;
    @Autowired
    private ReelSaveRepo reelSaveRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private ReelRepo reelRepo;
    private final RedisTemplate<String,String> redisTemplate;

    private String likeKey(String reelId){
        return "reel:likes:"+reelId;
    }
    private String commentKey(String reelId){
        return "reel:comment:"+reelId;
    }
    private String userLikesKey(String userId){
        return "user:likes:"+userId;
    }
    private String userSavedKey(String userId){
        return "user:saved:"+userId;
    }
    @Transactional
    public LikeResponse toggleLike(String userId,String reelId){
        boolean alreadyLiked = reelLiKeRepo.existsByUser_UserIdAndReel_ReelId(userId,reelId);
        if (alreadyLiked){
            reelLiKeRepo.deleteByUser_UserIdAndReel_ReelId(userId,reelId);
            redisTemplate.opsForValue().decrement(likeKey(reelId));
            redisTemplate.opsForSet().remove(userLikesKey(userId));
            long current= safeGetLong(redisTemplate.opsForValue().get(likeKey(reelId)));
            return new LikeResponse(false,current);
        }else{
            UserEntity user = userRepo.findByUserId(userId);
            ReelEntity reel = reelRepo.findByReelId(reelId);
            ReelLikeEntity like=new ReelLikeEntity();
            like.setUser(user);
            like.setReel(reel);
            like.setLikedAt(LocalDateTime.now());
            reelLiKeRepo.save(like);
            redisTemplate.opsForValue().increment(likeKey(reelId),1);
            redisTemplate.opsForSet().add(userLikesKey(userId),reelId);
            long current = safeGetLong(redisTemplate.opsForValue().get(likeKey(reelId)));
            return new LikeResponse(true,current);
        }
    }

    public long getLikesCount(String reelId){
        String likes= redisTemplate.opsForValue().get(likeKey(reelId));
        if (likes!=null) return Long.parseLong(likes);
        long count = reelLiKeRepo.countByReel_ReelId(reelId);
        redisTemplate.opsForValue().set(likeKey(reelId),String.valueOf(count));
        return count;
    }

    private long safeGetLong(String v){
        try { return v == null ? 0L : Long.parseLong(v); }
        catch(Exception e){ return 0L; }
    }
    @Transactional
    public CommentResponse addComment(String userId,String reelId,String text){
        UserEntity user = userRepo.findByUserId(userId);
        ReelEntity reel = reelRepo.findByReelId(reelId);

        ReelCommentEntity comment = ReelCommentEntity.builder()
                .user(user)
                .reel(reel)
                .comment(text)
                .commentedAt(LocalDateTime.now())
                .build();
        reelCommentRepo.save(comment);
        redisTemplate.opsForValue().increment(commentKey(reelId));
        return new CommentResponse(comment.getId(),
                user.getProfile().getUserName(),
                comment.getComment(),
                comment.getCommentedAt());
    }

    public Page<CommentResponse> getComments(String reelId,int page,int size){
        Pageable pageable= PageRequest.of(page,size, Sort.by("commentedAt").descending());
        Page<ReelCommentEntity> comments= reelCommentRepo.findByReel_ReelIdOrderByCommentedAtDesc(reelId,pageable);
        return comments.map(c->new CommentResponse(c.getId(),c.getUser().getProfile().getUserName(),c.getComment(),c.getCommentedAt()));
    }

    public long getCommentCounts(String reelId){
        String count= redisTemplate.opsForValue().get(commentKey(reelId));
        if (count!=null){
            return Long.parseLong(count);
        }
        Long numbersOfComments= reelCommentRepo.countByReel_ReelId(reelId);
        redisTemplate.opsForValue().set(commentKey(reelId),String.valueOf(numbersOfComments));
        return numbersOfComments;
    }

    public void deleteComment(String userId,Long commentId){
        ReelCommentEntity comment= reelCommentRepo.findById(commentId).orElseThrow(()->new RuntimeException("Comment Not Found"));
        if (!comment.getUser().getUserId().equals(userId)){
            throw new RuntimeException("User can not delete someone else comment");
        }
        String key = commentKey(comment.getReel().getReelId());
        String value = redisTemplate.opsForValue().get(key);
        long currentCount = value == null ? 0 : Long.parseLong(value);
        if (currentCount > 0) {
            redisTemplate.opsForValue().decrement(key, 1);
        }
        reelCommentRepo.delete(comment);
    }

    @Transactional
    public SaveResponse toggleSave(String userId,String reelId){
        boolean isAlreadySaved = reelSaveRepo.existsByUser_UserIdAndReel_ReelId(userId,reelId);
        if (isAlreadySaved){
            reelSaveRepo.deleteByUser_UserIdAndReel_ReelId(userId,reelId);
            redisTemplate.opsForSet().remove(userSavedKey(userId),reelId);
            return new SaveResponse(false);
        }
        UserEntity user = userRepo.findByUserId(userId);
        ReelEntity reel = reelRepo.findByReelId(reelId);
        ReelSaveEntity saved= ReelSaveEntity.builder()
                .user(user)
                .reel(reel)
                .savedAt(LocalDateTime.now())
                .build();
        reelSaveRepo.save(saved);
        redisTemplate.opsForSet().add(userSavedKey(userId),reelId);
        return new SaveResponse(true);
    }

    public Page<ReelResponse> getSavedReels(String userId, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("savedAt").descending());
        Page<ReelSaveEntity> pg = reelSaveRepo.findByUser_UserIdOrderBySavedAtDesc(userId, p);

        return pg.map(s -> {
            ReelEntity r = s.getReel();
            return new ReelResponse(r.getReelId(),r.getUser().getUserId(), r.getReelUrl(), r.getThumbnailUrl(), r.getDescription(), r.getCreatedAt().toString());
        });
    }

}
