/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.locapp.dao;

import java.util.List;
import java.util.logging.Level;

import org.locapp.dto.FileInfo;
import org.locapp.util.ValidationHelper;

/**
 *
 * @author daniel
 */
public class FileInfoFacade extends AbstractFacade<FileInfo> {

    public FileInfoFacade() {
        super(FileInfo.class);
    }

    public void saveFileInfos(List<FileInfo> files) throws DatabaseException {
        try {
            getEntityManager().getTransaction().begin();
            for (FileInfo file : files) {
                ValidationHelper.validateBean(file);
                getEntityManager().persist(file);
            }
            getEntityManager().getTransaction().commit();

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            if (getEntityManager().getTransaction().isActive()) {
                getEntityManager().getTransaction().setRollbackOnly();
            }
            throw new DatabaseException(e.getMessage(), e);
        }
    }
}
