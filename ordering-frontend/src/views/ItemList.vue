<template>
    <ItemListComponent
    :isAdmin="false"
    pageTitle = "ItemList"
    />
</template>

<script>
import ItemListComponent from '@/components/ItemListComponent.vue';
export default {
    props:['isAdmin','pageTitle'],
    components:{
        ItemListComponent
   }
}
</script>