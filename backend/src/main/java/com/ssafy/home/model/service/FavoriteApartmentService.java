package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.FavoriteApartment;

import java.util.List;

public interface FavoriteApartmentService {
    List<FavoriteApartment> list(int userId);
    FavoriteApartment add(int userId, String aptSeq);
    boolean remove(int userId, int favoriteId);
}

