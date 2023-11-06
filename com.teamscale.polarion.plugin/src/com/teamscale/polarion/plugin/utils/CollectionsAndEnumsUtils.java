package com.teamscale.polarion.plugin.utils;

import com.polarion.alm.tracker.model.IApprovalStruct;
import com.polarion.alm.tracker.model.IHyperlinkStruct;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Utility class for supporting the conversion of collections and enums. */
public class CollectionsAndEnumsUtils {

  /**
   * Takes a raw collection of Polarion hyperlinks and converts to a list of strings (link URIs).
   */
  public static List<String> castHyperlinksToStrList(Collection hyperlinks) {
    List<String> result = new ArrayList<>();
    if (isCollectionHyperlinkStructList(hyperlinks)) {
      try {
        List<IHyperlinkStruct> collection = (List<IHyperlinkStruct>) hyperlinks;
        result =
            collection.stream()
                .map(elem -> ((IHyperlinkStruct) elem).getUri())
                .collect(Collectors.toList());
      } catch (ClassCastException ex) {
        // casting should not be an issue since we're checking it on the if
        return result;
      }
    }
    return result;
  }

  /** This will return false if the list is empty, even if the list is of type IHyperlinkStruct */
  public static boolean isCollectionHyperlinkStructList(Collection collection) {
    if (collection instanceof List) {
      List<?> list = (List<?>) collection;
      if (!list.isEmpty() && list.get(0) instanceof IHyperlinkStruct) {
        return true;
      }
    }
    return false;
  }

  /**
   * This will return false if the list is empty, even if the list is of type
   * ILinkedWorkedItemStruct
   */
  public static boolean isCollectionLinkedWorkItemStructList(Collection collection) {
    if (collection instanceof List) {
      List<?> list = (List<?>) collection;
      if (!list.isEmpty() && list.get(0) instanceof ILinkedWorkItemStruct) {
        return true;
      }
    }
    return false;
  }

  /**
   * Takes a raw collection of Polarion approvalStructs and converts to a list of strings (the users
   * IDs).
   */
  public static List<String> castApprovalsToStrList(Collection approvals) {
    List<String> result = new ArrayList<>();
    if (isCollectionApprovalStructList(approvals)) {
      try {
        List<IApprovalStruct> collection = (List<IApprovalStruct>) approvals;
        result =
            collection.stream()
                .map(elem -> ((IApprovalStruct) elem).getUser().getId())
                .collect(Collectors.toList());
      } catch (ClassCastException ex) {
        // casting should not be an issue since we're checking it on the if
      }
    }
    return result;
  }

  /** This will return false if the list is empty, even if the list is of type IApprovalStruct */
  public static boolean isCollectionApprovalStructList(Collection collection) {
    if (collection instanceof List) {
      List<?> list = (List<?>) collection;
      if (!list.isEmpty() && list.get(0) instanceof IApprovalStruct) {
        return true;
      }
    }
    return false;
  }
}
