import React, { useEffect, useState } from "react";
import styles from "./PhotographerProfilePage.module.scss";
import {
   PhotographerDescription,
   PhotographerInfo,
   PhotographerReviewsCardWrapper,
} from "components/photographer-profile";
import { useGetPhotographerDetailedInfoQuery } from "redux/service/photographerService";
import { useParams } from "react-router-dom";

export const PhotographerProfilePage = () => {
   const { login } = useParams();
   const { data, isError } = useGetPhotographerDetailedInfoQuery(login);

   return (
      <section className={styles.photographer_info_page_wrapper}>
         <p className="category-title">{"photographer_page.category-title"}</p>
         {isError && <p>EXCEPTION</p>}
         {data && (
            <div className={styles.content}>
               <PhotographerInfo
                  name={data?.name}
                  surname={data?.surname}
                  location="Lodz"
                  stars={data?.score}
                  sessionCount={30}
                  photosCount={546}
                  reviewCount={data?.reviewCount}
               />

               <div className={styles.column}>
                  <PhotographerDescription
                     specializationList={data?.specializationList}
                     description={data?.description}
                  />

                  <PhotographerReviewsCardWrapper />
               </div>
            </div>
         )}
      </section>
   );
};
