package com.example.lostandfound.service;

import com.example.lostandfound.dto.ItemDTO;

import java.util.List;

public interface ItemService {
    ItemDTO.PublishResultVO createLost(ItemDTO.CreateItemRequest request);

    ItemDTO.PublishResultVO createFound(ItemDTO.CreateItemRequest request);

    List<ItemDTO.ItemSummaryVO> search(String keyword, String location, String type, Long categoryId);

    List<ItemDTO.ItemSummaryVO> keywordSearch(String keyword, String type, Long categoryId);

    List<ItemDTO.MyItemVO> myItems();

    ItemDTO.ItemActionVO updateItem(String itemId, ItemDTO.UpdateItemRequest request);

    ItemDTO.ItemActionVO offlineItem(String itemId);

    ItemDTO.ItemActionVO deleteItem(String itemId);

    ItemDTO.ItemActionVO reportItem(String itemId, String reason);

    ItemDTO.ItemActionVO claimFoundItem(String itemId, String message);

    ItemDTO.MyClaimsVO myClaims();

    ItemDTO.ItemActionVO approveClaim(Long claimId, String remark);

    ItemDTO.ItemActionVO rejectClaim(Long claimId, String remark);

    ItemDTO.ItemActionVO completeItem(String itemId);
}
