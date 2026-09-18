package com.campus.lab.service;

import com.campus.lab.common.BizException;
import com.campus.lab.entity.Announcement;
import com.campus.lab.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public List<Announcement> list() {
        return announcementRepository.findAllByOrderByPinnedDescCreatedAtDesc();
    }

    @Transactional
    public Announcement create(String publisher, Announcement body) {
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            throw new BizException("公告标题不能为空");
        }
        body.setId(null);
        body.setPublisher(publisher);
        body.setPinned(Boolean.TRUE.equals(body.getPinned()));
        body.setCreatedAt(java.time.LocalDateTime.now());
        return announcementRepository.save(body);
    }

    @Transactional
    public Announcement update(Long id, Announcement body) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BizException("公告不存在"));
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            throw new BizException("公告标题不能为空");
        }
        a.setTitle(body.getTitle());
        a.setContent(body.getContent());
        a.setPinned(Boolean.TRUE.equals(body.getPinned()));
        return announcementRepository.save(a);
    }

    @Transactional
    public void delete(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new BizException("公告不存在");
        }
        announcementRepository.deleteById(id);
    }
}
